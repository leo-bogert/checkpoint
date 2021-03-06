#!/bin/bash
set -o pipefail	# this is absolutely critical: make pipes exit with failure if any of the involved commands fails. the default is exit status of the last command.
set -o nounset	# exit with error upon access of unset variable

################################################################################################################################
# global variables
################################################################################################################################
declare CHECKPATH								# directory to generate checkpoint of
declare OUTPUTDIR								# directory to place checkpoint in
declare -A OUTPUTFILES							# files which will reside in the output directory
OUTPUTFILES[CHECKPOINT]=""						# the checkpoint itself
OUTPUTFILES[CHECKPOINT_OLDFORMAT_DATES]=""		# an oldformat date checkpoint - for testing the script against the old reference implementation
OUTPUTFILES[CHECKPOINT_OLDFORMAT_SHA256]=""		# an oldformat sha256 checkpoint - for testing the script against the old reference implementation
OUTPUTFILES[LOG]=""								# the log of stderr during computation. i

# the following associative arrays contain the output files.
# key=path of file, value=checkpoint line as it should appear in the output
declare -A OUTPUT_LINES_BY_PATH_WITHOUT_PREFIX	# output file OUTPUTFILES[CHECKPOINT]. we CANNOT store the \0 character in bash variables so we don't include the "filepath\0" prefix, we echo it to the outputfile instead.
declare -A OUTPUT_LINES_BY_PATH_DATE_ONLY		# output file OUTPUTFILES[CHECKPOINT_OLDFORMAT_DATES]
declare -A OUTPUT_LINES_BY_PATH_SHA256_ONLY		# output file OUTPUTFILES[CHECKPOINT_OLDFORMAT_SHA256]

declare ABORTED="false"							# will be set to "true" by the SIGHUP/SIGINT/SIGTERM trap to gracefully abort computation so resuming actually works
################################################################################################################################

log() {
	echo "$@" >&2
}

fail() {
	log "$@"
	exit 1
}

generate_files_and_directories() {
	(
		mkdir -p "$OUTPUTDIR" &&
		( [ "$(id -u -n)" != root ] || chown root:root "$OUTPUTDIR" ) &&
		chmod 700 "$OUTPUTDIR" &&
		touch "${OUTPUTFILES[@]}" &&
		chmod 600 "${OUTPUTFILES[@]}"
	) || fail "Output dir generation failed!"
}

redirect_stderr_to_log() {
	exec 2>> "${OUTPUTFILES[LOG]}" || fail "Redirecting stderr to ${OUTPUTFILES[LOG]} failed!"
}

trap_signals() {
	trap "interrupt_compute" HUP INT TERM  || fail "trap failed!"
}

set_ioniceness() {
	ionice -c 3 -p $$ || fail "ionice failed!"
}

load_existing_checkpoint() {
	log "Loading existing checkpoint data to resume from it ..."

	local filepath
	local line_without_filepath_prefix
	local split_line
	local -i count=0
	local -i ignored_count=0
	
	[ -s "${OUTPUTFILES[CHECKPOINT]}" ] || { log "No existing checkpoint found, creating a fresh one..." ; return 0 ; }
	
	while true ; do
		# The EOF marker is a line which contains one of the entries in the "case" statement below followed by \0 and nothing else.
		# So we should always be able to read something which is terminated by \0
		{ IFS= read -r -d $'\0' filepath ; } || fail "End of file marker not found - Input file is incomplete!"
		
		# Each line is composed of the filename followed by \0 followed by the checkpoint data followed by \n
		# If the checkpoint data cannot be read, we have reached the end of file. 
		# The "filename" should be the EOF marker then. So we check whether it is.
		if ! IFS= read -r line_without_filepath_prefix ; then
			case "$filepath" in
				$'This checkpoint is complete.\n')
					fail "Checkpoint is complete already, nothing to do. Exiting." ;;
				$'This checkpoint is INCOMPLETE but can be resumed.\n') 
					break ;; # The file was complete so we can finish the loop.
				*)
 					fail "End of file marker not found - Input file is incomplete!" ;;
			esac
		fi
		
		[[ "$line_without_filepath_prefix" != *'(sha256sum failed!)'* ]] || { let ++ignored_count ; continue ; }
		[[ "$line_without_filepath_prefix" != *'(stat failed!)'* ]] || { let ++ignored_count ; continue ; }
		
		OUTPUT_LINES_BY_PATH_WITHOUT_PREFIX["$filepath"]="$line_without_filepath_prefix"
		
		# new data is complete.
		# we don't read the old format files. instead we compute them from the new format data.
		# - they could never be parsed correctly because they delimit the filename with "\n" which is a valid character in linux filenames.
		
		# Split the line by \t
		IFS=$'\t' read -r -a split_line <<< "$line_without_filepath_prefix"
		
		local sha="${split_line[0]}"

		# the times array was split by tabs so we merge it to a single variable with tab as separator again
		IFS_OLD="$IFS"
		IFS=$'\t'	# we cannot do the "assign variable only for one command" thing because what we want to do (the following line) is not a command but an assignment itself. See "SIMPLE COMMAND EXPANSION" in bash manpage.
		local times="${split_line[*]:1}"
		IFS="$IFS_OLD"
		
		[ "$sha" != "(directory)" ] && OUTPUT_LINES_BY_PATH_SHA256_ONLY["$filepath"]="$sha *$filepath"
		OUTPUT_LINES_BY_PATH_DATE_ONLY["$filepath"]="$filepath"$'\t'"$times"
		
		let ++count
	done < "${OUTPUTFILES[CHECKPOINT]}"
	
	log "Loaded $count existing checkpoint datasets, ignored $ignored_count existing datasets where sha256sum or stat had failed previously."
}

interrupt_compute() {
	ABORTED=true	
}

compute() {
	log "Computing checkpoint ..."

	# We run find inside the target directory so the filenames in the output are relative
	cd "$CHECKPATH" || fail "cd to $CHECKPATH failed!"
	
	local file
	local -i computed=0
	local -i failed=0
	local -i skipped=0
	
	# Source of this magic: http://mywiki.wooledge.org/BashFAQ/020
	# IFS= to prevent trimming the string, ie. removing leading/trailing whitespace
	# -r = Backslash does not act as an escape character
	# -d = Sets the delimiter to null
	while IFS= read -r -d $'\0' file; do
		[ "$ABORTED" = "false" ] || { log "Aborting computation due to signal!" ; break ; }
		[ ${#OUTPUT_LINES_BY_PATH_WITHOUT_PREFIX["$file"]} -eq 0 ] || { let ++skipped ; continue ; }
		
		local shaoutput=""
		local sha=""
		local stat=""

		# Compute the sha256sum sum. Notice that we do not abort if computation fails:
		# The script is aimed to be run on very large datasets, especially complete fileservers.
		# Therefore, computation will take a very long time and the probability is high that a file on the system is deleted during processing.
		if [ -f "$file" ] ; then
			shaoutput="$(sha256sum --binary "$file")" && read -r -a sha_array <<< "$shaoutput" || { sha_array[0]="(sha256sum failed!)" ; let ++failed ; }
			sha="${sha_array[0]}"
		elif [ -d "$file" ] ; then
			sha="(directory)"
		elif [ ! -e "$file" ]; then
			log "File deleted during processing: $file"
			let ++failed
			continue
		else
			fail "Unexpected type of file: $file"
		fi
		
		stat="$(stat --printf "Birth: %w\tAccess: %x\tModify: %y\tChange: %z" "$file")" || { stat="(stat failed!)" ; let ++failed ; }
		
		# new format
		OUTPUT_LINES_BY_PATH_WITHOUT_PREFIX["$file"]=$'\t'"$sha"$'\t'"$stat"
		
		# old format of date listing
		OUTPUT_LINES_BY_PATH_DATE_ONLY["$file"]="$file"$'\t'"$stat"
		# old format of sha listing
		[ -f "$file" ] && OUTPUT_LINES_BY_PATH_SHA256_ONLY["$file"]="$sha *$file"

		let ++computed
	done < <(find . -mount \( -type f -o -type d \) -print0)
	
	log "Computing finished. Computed $computed entries successfully. $failed computations failed. Skipped $skipped of ${#OUTPUT_LINES_BY_PATH_WITHOUT_PREFIX[@]} files due to incremental computation."
}

truncate_output() {
	> "${OUTPUTFILES[CHECKPOINT]}"
	> "${OUTPUTFILES[CHECKPOINT_OLDFORMAT_DATES]}"
	> "${OUTPUTFILES[CHECKPOINT_OLDFORMAT_SHA256]}"
}

write_eof_marker_to_output() {
	for outputfile in "${OUTPUTFILES[@]}" ; do
 		{ [ "$ABORTED" = "false" ] && echo -n -e "This checkpoint is complete.\n\0" || echo -n -e "This checkpoint is INCOMPLETE but can be resumed.\n\0" ; } >> "$outputfile"
	done
}

err_handler() {
	fail "error at line $1" "exit code is $2"
}

enable_errexit_and_errtrace() {
	set -o errexit
	set -o errtrace
	trap 'err_handler "$LINENO" "$?"' ERR
}

disable_errexit_and_errtrace() {
	trap - ERR
	set +o errexit
	set +o errtrace
}

write_output() {
	log "Writing checkpoint to disk ..."

	enable_errexit_and_errtrace

		truncate_output

		local -i count=0
		local file
		while IFS= read -r -d $'\0' file ; do
			{
				echo -n "$file"
				echo -n -e "\0"
				echo "${OUTPUT_LINES_BY_PATH_WITHOUT_PREFIX[$file]}" ;	# "${array[$key]}" is sufficient, quoting of the key not needed
			} >> "${OUTPUTFILES[CHECKPOINT]}"
			
			# old format with date only
			echo "${OUTPUT_LINES_BY_PATH_DATE_ONLY[$file]}" >> "${OUTPUTFILES[CHECKPOINT_OLDFORMAT_DATES]}"
			
			# old format with sha only. may not be defined for directories
			[ ${#OUTPUT_LINES_BY_PATH_SHA256_ONLY["$file"]} -ne 0 ] && echo "${OUTPUT_LINES_BY_PATH_SHA256_ONLY[$file]}" >> "${OUTPUTFILES[CHECKPOINT_OLDFORMAT_SHA256]}"

			# HOWTO shoot yourself in the feet with "set -o errexit" and "let":
			# "help let" says: "If the last ARG evaluates to 0, let returns 1; let returns 0 otherwise."
			# The return status of 1 would also apply to "let count++" since the value of $count is evaluated before the increment due to the post-increment operator and $count is initialized to 0.
			# So we have to use pre-increment because we use "set -o errexit" and the exit failure of "let count++" would trigger it.
			let ++count

			# LC_ALL=C is critically important to ensure files of the same
			# directory will be next to each other in the output.
			# As a bonus it also ensures the sorting is always the same
			# indepedent of the local system's language configuration.
		done < <(
				for filepath in "${!OUTPUT_LINES_BY_PATH_WITHOUT_PREFIX[@]}" ; do
					echo -n "$filepath"
					echo -n -e "\0"
				done | LC_ALL=C sort --zero-terminated
			)

		write_eof_marker_to_output

	disable_errexit_and_errtrace

	log "Finished writing checkpoint to disk. Wrote $count entries."
}


main() {
	enable_errexit_and_errtrace
	
		# TODO: Fix this
		echo "WARNING: This likely is NOT safe for usage upon arbitrary files, which might have dashes in their names, because it uses echo on their names instead of printf!" >&2

		[ $# -eq 2 ] || fail "Syntax: checkpoint <dir to generate checkpoint for> <output dir of checkpoint files>"

		local checkpath_tmp="$1"
		local outputdir_tmp="$2"
		
		# strip trailing slash as long as the dir is not "/"
		[[ "$checkpath_tmp" != "/" ]] && checkpath_tmp="${checkpath_tmp%/}"
		[[ "$outputdir_tmp" != "/" ]] && outputdir_tmp="${outputdir_tmp%/}"
		# make the directories absolute
		[[ "$checkpath_tmp" != /* ]] && checkpath_tmp="$(pwd)"/"$checkpath_tmp"
		[[ "$outputdir_tmp" != /* ]] && outputdir_tmp="$(pwd)"/"$outputdir_tmp"
		
		readonly CHECKPATH="$checkpath_tmp"
		[ -d "$CHECKPATH" ] || fail "Dir to generate checkpoint for does not exist or is no directory: $CHECKPATH"
		
		readonly OUTPUTDIR="$outputdir_tmp"
		OUTPUTFILES[CHECKPOINT]="$OUTPUTDIR/checkpoint.txt"
		OUTPUTFILES[CHECKPOINT_OLDFORMAT_DATES]="$OUTPUTDIR/filedates.txt"
		OUTPUTFILES[CHECKPOINT_OLDFORMAT_SHA256]="$OUTPUTDIR/files.sha256"
		OUTPUTFILES[LOG]="$OUTPUTDIR/errors.log"	
		readonly OUTPUTFILES
		
	disable_errexit_and_errtrace  # don't exit upon error. this is necessary for compute() especially because some files might be deleted during our huge execution time
		
	generate_files_and_directories # generate output dir before writing to logfile since it resides in it
	redirect_stderr_to_log
	trap_signals # trap signals after starting to log so we can log them
	set_ioniceness
	load_existing_checkpoint
	# TODO: write & call a "remove_inexistant_files" which removes files which were imported from the existing checkpoint but don't exist anymore
	compute
	write_output
}

main "$@"
