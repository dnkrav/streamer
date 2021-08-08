# About

## Streamer Functionality

* Read list of video files uploaded in certain folder on the server,
* Create various Playlists by picking and ordering selected files,
* Generate a text file with playlist for ffmpeg [concatenation](https://trac.ffmpeg.org/wiki/Concatenate "ffmpeg Documentation"),
* Run ffmpeg stream ([copy](https://ffmpeg.org/ffmpeg.html#Stream-copy "ffmpeg Documentation") mode) of selected Playlist on specific RTMP resource in a background,
* Stop ffmpeg streaming.

## lsFusion Platform resources

* https://lsfusion.org
* [Slack Discussion](https://slack.lsfusion.org/)

---

# HowTo

## Working Forms

* **Playlists** - main form to work with, all target functionality is there
* **Master data** / **ffmpeg Parameters** - application settings
* **Account** - user authentication settings

## Application settings

* **Parameters of ffmpeg command** - command line arguments for the ffmpeg runner *(not implemented yet)*
* **Absolute path to local storage of video files** - path to video storage on the server' filesystem
* **Folders for video files** - subfolders in the storage:
   + **manual** - files uploaded using SSH connection
   + **auto** - files uploaded using GUI
* **Streaming resource** - RTMP address for streaming

## Workflow on the Playlists form

* **Playlist** - list of created playlists
   + **playing** - shows if this playlist is streaming currently
   + **Created** - date/time of the last edition of the playlist
   + **Name** - custom name of the playlist
   + **Count Videos** - number of videos in the playlist
   + **Play** - button to start the streaming
   + **Stop** - button to stop the streaming
* Buttons **Add**, **Edit**, **Delete** - run named Editor for selected playlist
* **Video files** - list of videos within the selected Playlist

## Playlist Editor

* **Name** - set custom name for the playlist
* **in** - tick if the video file should be used in the current playlist
* **Order** - optional number of the video file in the playlist queue
* **File Name** - video file name on the server
* **Delete file** - remove the video from the list, but keep it on the server
* Button **Upload a new video file** - use GUI to upload file on the server *(not implemented yet)*
* Button **Check for new manually uploaded files** - screen the manual folder on the server and detect new files there
* Buttons **Save**, **OK**, **Cancel**, **Close** - save changes in the database and generate updated playlist.  
   *Important: without pressing one of these buttons no changes will be stored in case of closing window or disconnect from the application.*
* **Playlist** - preview of the playlist resulting the selected preferences

---

# Troubleshooting

## Illegal seek

ffmpeg from the repo of Debian 10 is of version 4.1.6: running infinity loop on it using concat demuxer crushes with error 'Illegal seek' in the end of the first cycle.  
A newer version is needed.  Successfully tested on version 4.4. Installing ffmpeg from sources is described in details in it's [Documentation](https://trac.ffmpeg.org/wiki/CompilationGuide/Ubuntu).  
After building from sources the newer version should be installed for the 'lsfusion' user as well, because the application is running from him.  E.g. copy the binaries into common location and check it:  

    $ sudo cp ./bin/ff* /usr/local/bin/
    $ sudo -u lsfusion ffmpeg -version
