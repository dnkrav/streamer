# A *cron* job might be used to convert videos using ffmpeg
# Run from the _manual_ folder
# Check whether another ffmpeg task is running currently
cd /mnt/video/manual
if ! (ps -A | grep ffmpeg)
then
  mkdir -p flv
  mkdir -p arch
  # Check whether there is a file to convert
  number_mov=$(find mov/*.mov | wc -l)
  if (( $number_mov>0 ))
  then
    # Convert single file
    movfile=$(ls mov/*.mov | head -1)
    flvfile="${movfile##*/}"
    # Change extension
    flvfile="${flvfile%mov}flv"
    # Move converted file into a backup
    bakfile="arch/${movfile##*/}"
    ffmpeg -i "$movfile" -filter:v fps=30 -video_track_timescale 1k -c:v libx264 -preset medium -b:v 3000K -maxrate 3000k -bufsize 6000k -g 60 -c:a aac -b:a 128k -ac 2 -ar 44100 -y "$flvfile"
    chown lsfusion:lsfusion "$flvfile"
    mv "$flvfile" "flv/$flvfile"
    mv "$movfile" "$bakfile"
  fi
fi