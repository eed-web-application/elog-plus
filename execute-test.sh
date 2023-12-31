# #!/bin/bash
# file_array=()
# # Find all files with names starting with 'docker-compose.'
# files=$(find $PWD -name 'docker-compose*')

# # Iterate over the files
# for file in $files; do
#   echo "Add $file"
#    file_array+=" -f $file"
#   # Your processing code here...
# done

# # Check if the array is empty
# if [ -z "${file_array[*]}" ]; then
#   echo "No files found"
# else
#   # Call your command with all the file paths as arguments
#   echo 'execute => docker compose ${file_array[@]} up --attach test  --exit-code-from test'
  
# fi

#docker compose "${file_array[@]}" up  test  --exit-code-from test
docker compose -f ./docker-compose.yml -f ./docker-compose-test.yml up  test  --exit-code-from test