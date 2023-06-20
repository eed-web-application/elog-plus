#!/bin/bash
file_array=()
# Find all files with names starting with 'docker-compose.'
files=$(find $PWD -name 'docker-compose.*')

# Iterate over the files
for file in $files; do
  echo "Add $file"
   file_array+=" -f $file"
  # Your processing code here...
done

# Check if the array is empty
if [ -z "${file_array[*]}" ]; then
  echo "No files found"
else
  # Call your command with all the file paths as arguments
  docker compose "${file_array[@]}" up --attach test  --exit-code-from test
fi

