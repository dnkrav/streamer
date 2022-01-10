# About

Streamer helps to manage video streaming on a resource, which supports RTMP protocol.
The interface allows to combine and sort video files in playlists, which then to be selected for running in infinity loop.
The ffmpeg streaming is runnning as an operating system service and controlled by systemd.
It uses ffmpeg in Stream copy mode, so no CPU resources spent on video conversion when playing every video many times.
The videos uploaded by user are being automatically converted into FLV format for this purpose.

## Streamer Functionality

* Read list of video files uploaded in certain folder on the server,
* Convert uploaded video into FLV format, if needed, in background task,
* Create various Playlists by picking and ordering selected files,
* Generate a text file with playlist for ffmpeg [concatenation](https://trac.ffmpeg.org/wiki/Concatenate "ffmpeg Documentation"),
* Trigger operating system service to run ffmpeg stream ([copy](https://ffmpeg.org/ffmpeg.html#Stream-copy "ffmpeg Documentation") mode) of selected Playlist on specific RTMP resource in a background,
* Stop ffmpeg streaming.

## lsFusion Platform resources

* https://lsfusion.org
* [Slack Discussion](https://slack.lsfusion.org/)

---

# HowTo

## Working Forms

* **Playlists** - main form to work with, all target functionality is there,
* **ffmpeg Parameters** - application settings (in **Master data** menu),
* **Scheduler** - background video conversion (in **Administration** menu),
* **Account** - user authentication settings.

## Application settings

<!-- * **Parameters of ffmpeg command** - command line arguments for the ffmpeg runner *(not implemented yet)* -->
* **Absolute path to local storage of video files** - path to video storage on the server' filesystem, must be created and specified by server Administrator;
* **Absolute path to pid file for playlist service** - path set as PID_FILE in the operation system service
* **Absolute path to symbolic link, which targets service on to the playlist file** - the file used by the operation system service
* **Folders for video files** - subfolders in the storage:
   + **manual** - files uploaded using SSH connection,
   + **auto** - files uploaded using GUI,
   + **convert** - files, identified by application for format conversion,
   + **flv** - files picked by application for streaming;
* **Streaming resource** - RTMP address for streaming;
* **Videos under conversion** - number of uploaded videos in non-FLV format; 
* **Playlist is running** - name of currently running playlist;
* **ffmpeg process is busy** - flag of running playlist or video conversion, may be used to block any ffmpeg function.

## Workflow on the Playlists form

* **Playlist** - list of created playlists:
   + **playing** - shows if this playlist is streaming currently,
   + **Created** - date/time of the last edition of the playlist,
   + **Name** - custom name of the playlist,
   + **Count Videos** - number of videos in the playlist,
   + **Play** - button to start the streaming of selected Playlist,
   + **Stop** - button to stop the streaming,
* Buttons **Add**, **Edit**, **Delete** - run named Editor for selected playlist;
* **Video files** - list of videos within the selected Playlist.

## Playlist Editor

* **Name** - set custom name for the playlist.
* Video files list:
   * **in** - tick if the video file should be used in the current playlist,
   * **Order** - optional number of the video file in the playlist queue,
   * **Uploaded** - date/time when the video file was registered in the application,
   * **File Name** - video file name on the server,
   * **Delete file** - remove the video from the list and from the server, it is available only when a tick **Show deletion button** is switched on.
* Button **Upload a new video file** - user dialog to upload a video file on the server, the filename to store the file on the server after upload must be provided;
* Button **Check for new manually uploaded files** - screen the manual folder on the server and detect new files there;
* Autofilter **Show files in FLV format only** - switch between viewing files in different formats.  
   Non-converted files may be added into a playlist, they will appear in generated file after conversion will be completed, until this Playlist Preview field is available.
* Buttons **Save**, **OK**, **Cancel**, **Close** - save changes in the database and generate updated playlist.  
   *Important: without pressing one of these buttons no changes will be stored in case of closing window or disconnect from the application.*
* **Playlist** - preview of the playlist resulting the selected preferences
<!--* Button **Upload a new video file** - use GUI to upload file on the server *(not implemented yet)*-->


## Video Converter

The video Converter runs in Scheduler.
It checks whether any newly uploaded videos need to be converted into FLV format.
Get next video from the conversion queue (if any) and runs ffmpeg job, if there is no active video streaming. 

The Scheduler need to be configured by the server admin:

1. Go **Administration** -> **Scheduler** -> **Tasks**.
2. **+Add** new Scheduler task.
3. Type desired **Name**, select **Scheduler start type** *From finish previous*, set **Repeat each** *60* **seconds**, select current date and time as **Start date**.
4. **+Add** new Scheduled task row.
5. Find **Action** *Streamer.convertNextVideo[]* aka *Convert next video*.
6. Tick **Active** flags for the row and the Task itself.
7. **Save** your changes.
8. Press button **Perform a task**.

---

# Troubleshooting

## Illegal seek

ffmpeg from the repo of Debian 10 is of version 4.1.6: running infinity loop on it using concat demuxer crushes with error 'Illegal seek' in the end of the first cycle.  
A newer version is needed.  Successfully tested on version 4.4. Installing ffmpeg from sources is described in details in it's [Documentation](https://trac.ffmpeg.org/wiki/CompilationGuide/Ubuntu).  
After building from sources the newer version should be installed for the 'lsfusion' user as well, because the application is running from him.  E.g. copy the binaries into common location and check it:  

    $ sudo cp ./bin/ff* /usr/local/bin/
    $ sudo -u lsfusion ffmpeg -version

## Playlist doesn't run a video file

The Video file should be of FLV format in order to eliminate video conversion workload at every streaming.  
The streamer defines the video format at upload by the filename extension and convert the video into FLV if it doesn't match "*.flv".

## Manual video files handling

Let say we need manually to process a bulk upload of files distributed in a subfolders hierarchy in the *manual* folder.

A *bash* script to extract files from subfolders is created for this purpose: *conf/move-from-subfolders.sh*

A *cron* job might be used then to convert videos using ffmpeg command: *conf/mov2flv_convert.sh*

Settings for the cron job in the */etc/cron.d/mov2flv* to try the script every minute:

    */1 * * * * lsfusion /bin/bash /mnt/video/manual/mov2flv_convert.sh