# Let say we need manually to process a bulk upload of files distributed in a subfolders hierarchy inside the _manual_ folder.

# Recursive processing of all files and solders
function process_subfolders() {
  # First process as a file, because it doesn't involves recursion
  if [[ -f $1 ]]
  then
    # Amend filename
    new_name="$depthDIR_${PWD##*/}_FILE_$1"
    # Move file up into _manual_ folder
    echo "Move file $1 into $new_name"
    sudo mv "$1" "$new_name"
    # Assign rights for the application
    sudo chown lsfusion:lsfusion "$new_name"
  fi
  # The variable $1 is the same here
  if [[ -d $1 ]]
  then
    # Step into a subfolder
    cd "$1"
    depth=$(echo "../$depth")
    echo "Entering folder: $PWD"
    # Precess all items
    for sd in *
    do
      process_subfolders "$sd"
    done
    # Step out from a subfolder
    cd ../
    depth=${depth#../}
    echo "Step out into folder: $PWD"
  fi
}

# Run from the _manual_ directory
# Put output in the _mov_ subfolder
cd /mnt/video/manual
mkdir -p mov
depth="mov/"
for d in *
do
  # don't touch existing files in the manual folder
  if [[ -d $d ]]
  then
    echo "$d"
    process_subfolders "$d"
  fi
done