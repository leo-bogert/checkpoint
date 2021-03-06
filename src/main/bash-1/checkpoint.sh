#!/bin/bash

if [ $# -ne 2 ] ; then
	echo "Syntax: checkpoint <dir to generate checkpoint for> <output dir of checkpoint files>"
	exit 1
fi

CHECKPATH="$1"
OUTPUTDIR="$2"

if [ ! -d "$CHECKPATH" ]; then
	echo "Dir to generate checkpoint for does not exist: $CHECKPATH" >&2
	exit 1
fi

SHA256="$OUTPUTDIR/files.sha256"
DATES="$OUTPUTDIR/filedates.txt"
ERRORS="$OUTPUTDIR/errors.txt"


compress()
{
	bzip2 -z -q -9 "$1"
}

rotate()
{
	if [[ -e "$1" ]]; then
		compress "$1" &&
		savelog -m 600 -u root -c 30 -l "$1.bz2" 1> /dev/null &&
		rm -f "$1.bz2"
	fi
}

ionice -c 3 -p $$ &&
mkdir -p "$OUTPUTDIR" &&
chown root:root "$OUTPUTDIR" &&
chmod 700 "$OUTPUTDIR" &&
rotate "$SHA256" &&
rotate "$DATES" &&
rotate "$ERRORS" &&
touch "$SHA256" "$DATES" "$ERRORS" &&
chmod 600 "$SHA256" "$DATES" "$ERRORS" &&
cd "$CHECKPATH" &&
( find . -mount \( -type f -o -type d \) -print0 | LC_ALL=C sort --stable -z | xargs -0 -n 1 stat --printf "%n\tBirth: %w\tAccess: %x\tModify: %y\tChange: %z\n" ) 1> "$DATES" 2>> "$ERRORS" &&
( find . -mount -type f -print0 | LC_ALL=C sort --stable -z | xargs -0 -n 1 sha256sum -b ) 1> "$SHA256" 2>> "$ERRORS"

if [ $? -ne 0 -o -s "$ERRORS" ] ; then
	echo "Last exit code indicates failure! Errors log file contents:" >&2
	cat "$ERRORS" 1>&2 # print all errors to stderr - since we run as a cronjob, this will be mailed to root automatically
	exit 1
fi
