#!/bin/bash

# Create the output directory if it doesn't exist (optional)
# mkdir -p ../solutions

# Loop through all scramble files in the testcases directory
for file in ../testcases/scramble*.txt; do
    # Check if the file exists (in case no files match the pattern)
    if [ ! -f "$file" ]; then
        echo "No scramble files found in ../testcases"
        exit 1
    fi

    # Extract the filename from the path
    filename=$(basename "$file")
    
    # Extract the number from the filename (e.g., scramble01.txt -> 01)
    # This uses regex to capture the digits
    if [[ $filename =~ scramble([0-9]+)\.txt ]]; then
        number="${BASH_REMATCH[1]}"
        
        # Construct output path
        output_path="sol${number}.txt"
        
        echo -e "\033[0;36mSolving $file -> $output_path\033[0m" # Cyan color
        
        # Capture start time (nanoseconds)
        start_time=$(date +%s%N)
        
        # Run the Java solver
        # Assuming compiled classes are in the current directory or 'bin'
        # Adjust classpath (-cp) as needed. '.' means current directory.
        java -cp . rubikscube.Solver "$file" "$output_path"
        
        # Capture end time
        end_time=$(date +%s%N)
        
        # Calculate duration in seconds (with decimal precision)
        duration=$((end_time - start_time))
        duration_sec=$(echo "scale=3; $duration / 1000000000" | bc)
        
        echo -e "\033[0;32mFinished in ${duration_sec} seconds.\033[0m" # Green color
        echo "----------------------------------------"
    fi
done

echo -e "\033[1;33mAll tests completed.\033[0m" # Yellow color