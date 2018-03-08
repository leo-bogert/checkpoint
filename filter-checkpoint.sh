#!/bin/bash
# Generic bash options which I always use for safety. Not all may be needed for this particular script.
set -o nounset
set -o pipefail
set -o errexit
set -o errtrace
trap 'echo "Error at line $LINENO, exit code is $?" >&2' ERR
shopt -s nullglob
shopt -s failglob

# These arrays contain regular expressions!
# So please be careful with using any non-letter characters, they may be reserved by grep and need to be escaped with \ then!
# See e.g. https://www.regular-expressions.info/refcharacters.html (Literal chracters in GNU BRE = what is safe).
# If in doubt test your regexp by kdiff3'ing a filtered checkpoint against its original version.
INCLUDE=( '^\./home/'  )
EXCLUDE=( '^\./home/some-user/' )

INCLUDE+=( '^This checkpoint is complete\.$' )
INCLUDE+=( '^This checkpoint is INCOMPLETE but can be resumed\.$' )
grep --text --file=<(printf '%s\n' "${INCLUDE[@]}") -- "$1/checkpoint.txt" |
grep --text --file=<(printf '%s\n' "${EXCLUDE[@]}") --invert-match > "$1/checkpoint.txt.filtered"
