#!/bin/bash
RESULT_FILE=$1

if [ -f $RESULT_FILE ]; then
  rm $RESULT_FILE
fi
touch $RESULT_FILE

checksum_file() {
  echo $(openssl md5 $1 | awk '{print $2}')
}

FILES=()
while read -r -d ''; do
  FILES+=("$REPLY")
done < <(find . -type f \( -name "build.gradle*" -o -name "Dependencies.kt" -o -name "gradle-wrapper.properties" \) -print0)

# Loop through files and append MD5 to result file
for FILE in ${FILES[@]}; do
  echo $(checksum_file $FILE) >> $RESULT_FILE
done
# Now sort the file so that it is 
sort $RESULT_FILE -o $RESULT_FILE