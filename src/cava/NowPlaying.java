package cava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JLabel;

import cava.server.ServerTrack;

public class NowPlaying extends Playlist {

    private ArrayList<Track> tracks = new ArrayList<Track>();
    private ArrayList<Integer> tracksplayed = new ArrayList<Integer>();
    private int position;
    private JLabel nowPlayingTrack;
    private JLabel nowPlaylingArtist;
    private ArtistContainer artistContainer;
    private boolean isPlaying;
    public static Boolean shuffle = false, repeat = false;
    TrackDatabase db;

    private NowPlaying(int playlistID, String playlistName, int numTracks) {
        super(playlistID, playlistName, numTracks);
    }

    NowPlaying(JLabel currentTrack, JLabel currentArtist) {
        super(-1, "Now Playing", 0);
	//m = new Marquee(currentTrack.getText());
        this.nowPlayingTrack = Marquee.movingText(currentTrack);
        this.nowPlaylingArtist = currentArtist;
        db = new TrackDatabase();
    }

    @Override
    public int getNumTracks() {
        if (tracks == null) {
            return 0;
        }
        return tracks.size();
    }

    public Track[] getTracksInPlaylist() {
        if (tracks != null) {
            return tracks.toArray(new Track[0]);
        } else {
            return new Track[0];
        }
    }

    @Override
    public Track[] getTracksInPlaylist(TrackDatabase db) {
        return getTracksInPlaylist();
    }

    public void setTracks(Track[] tracks) {
        this.tracksplayed.clear();
        this.tracks.clear();
        for (Track t : tracks) {
            this.tracks.add(t);
        }
        this.numTracks = this.tracks.size();
    }

    public void setTracks(ArrayList<Track> tracks) {
        this.tracksplayed.clear();
        this.tracks = tracks;
        this.numTracks = tracks.size();
    }

    public void addTrack(Track track) {
        tracks.add(track);
        numTracks++;
    }

    public void addTracks(Track[] tracks) {
        for (Track track : tracks) {
            this.tracks.add(track);
            numTracks++;
        }
    }

    public void addTracks(Track[] tracks, int indexToAddAt) {
        ArrayList<Track> newTracks = new ArrayList<Track>();
        //Copy tracks up until indexToAddAt
        for (int i = 0; i < indexToAddAt; i++) {
            newTracks.add(this.tracks.get(i));
        }
        //Add new tracks
        for (Track track : tracks) {
            newTracks.add(track);
        }
        //Add tracks after indexToAddAt
        for (int i = indexToAddAt; i < this.tracks.size(); i++) {
            newTracks.add(this.tracks.get(i));
        }

        //Adjust position if we inserted before it
        if (indexToAddAt <= position) {
            position += tracks.length;
        }

        this.tracks = newTracks;

    }

    public void playNext(Track[] tracks) {
        addTracks(tracks, position + 1);
    }

    public void setPosition(int position) {
        this.position = position;
        if (validPlayPosition()) {
            this.isPlaying = true;
        }
    }

    public int getPosition() {
        return this.position;
    }

    public boolean validPlayPosition() {
        //System.out.println( tracksplayed.size() );
        if (tracks == null) {
            return false;
        }
        if (this.position < tracks.size() && this.position >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public void skipForward() {
        //System.out.print( "am i getting here?" );
        if (shuffle == false) {
            // Shuffle OFF Repeat OFF
            if (repeat == false) {
                position++;
            } else {
                // Shuffle OFF Repeat ON
                if (position + 2 > tracks.size()) {
                    position = 0;
                } else {
                    position++;
                }
            }
        } else {
            if (tracksplayed.contains(position) == false) {
                tracksplayed.add(position);
            }
            Random r = new Random();
            int temp = 0;
            int count = 0;
            do {
                temp = r.nextInt(tracks.size());
                count++;
            } while (tracksplayed.contains(temp) == true && count < tracks.size());

            // Shuffle ON Repeat OFF
            if (repeat == false) {
                if (count < tracks.size()) {
                    position = temp;
                    tracksplayed.add(new Integer(temp));
                } else {
                    // If all tracks have been played, deliberately set position to an invalid value
                    position = tracks.size() + 1;
                }
            } else {
                // Shuffle ON Repeat ON
                if (count < tracks.size()) {
                    position = temp;
                    tracksplayed.add(new Integer(temp));
                } else {
                    // Make sure we don't play the same song as last time, any other one is fine
                    do {
                        temp = r.nextInt(tracks.size());
                        count++;
                    } while (temp == position);

                    position = temp;
                    tracksplayed.clear();
                    tracksplayed.add(temp);
                }
            }

        }
        //System.out.println( "\nWe just set the position to " + position + "\n" );
    }

    public void skipBack() {

        if (shuffle == false) {
            // Shuffle OFF Repeat OFF
            if (repeat == false) {
                if (position - 1 < 0) {
                    position = 0;
                } else {
                    position--;
                }
            } else {
                // Shuffle OFF Repeat ON
                if (position - 1 < 0) {
                    position = tracks.size() - 1;
                } else {
                    position--;
                }
            }
        } else {
            // Shuffle ON Repeat OFF
            if (repeat == false) {
                // If the tracksplayed arraylist is not empty, then we traverse it in reverse. If it is empty, we just jump to a random track. We don't keep track of what tracks we've skipped in reverse - the only check is to make sure we don't try to skip back to the same track we're already on
                if (tracksplayed.size() != 0) {
                    tracksplayed.remove(tracksplayed.size() - 1);
                    position = (Integer.parseInt(tracksplayed.get(tracksplayed.size() - 1).toString()));
                    //System.out.println("position is " + position);
                } else {
                    // Deliberately set to invalid value
                    position=0;
                }
            } else {
                // Shuffle ON Repeat ON
                // If the tracksplayed arraylist is not empty, then we traverse it in reverse. If it is empty, we just jump to a random track. We don't keep track of what tracks we've skipped in reverse - the only check is to make sure we don't try to skip back to the same track we're already on
                if (tracksplayed.size() >= 2) {
                    tracksplayed.remove(tracksplayed.size() - 1);
                    position = (Integer.parseInt(tracksplayed.get(tracksplayed.size() - 1).toString()));
                    //System.out.println("position is " + position);
                } else {
                    Random r = new Random();
                    int temp = position;
                    while (temp == position) {
                        temp = r.nextInt(tracks.size());
                    }
                    position = temp;
                }
            }

        }
        //System.out.println( "\nWe just set the position to " + position + "\n" );
    }

    public Track getCurrentTrack() {
        if (validPlayPosition()) {
            return tracks.get(position);
        } else {
            return null;
        }
    }

    public void setArtistContainer(ArtistContainer artistContainer) {
        this.artistContainer = artistContainer;
    }

    public void updateText() {
        if (this.validPlayPosition() && this.isPlaying) {
            String TrackName = tracks.get(position).getTrackName();
            /*if (TrackName.length() > 32) {
                //TrackName = TrackName.substring(0, 32);
                //Marquee m = new Marquee(TrackName + "     ");
                nowPlayingTrack = m.movingText(nowPlayingTrack);
		System.out.println( "nowPlayingTrack: " + nowPlayingTrack.getText() );
            } else {
		    nowPlayingTrack.setText(TrackName);
	    }*/
	    if( TrackName.length() < 33) Marquee.marqueeText = TrackName;
	    	else Marquee.marqueeText = "     " + TrackName + "     ";
            if (this.artistContainer != null) {
                String artistName;
                if (tracks.get(position).isClientTrack()) {
                    //Try to get artist from artist container hash table. Otherwise, load it from the database
                    Artist artist = artistContainer.getArtistByID(((ClientTrack) tracks.get(position)).getArtistID());
                    artistName = (artist != null) ? artist.getArtistName() : ((ClientTrack) tracks.get(position)).getArtistName(db);
                } else {
                    artistName = ((ServerTrack) tracks.get(position)).getArtistName();
                }
                this.nowPlaylingArtist.setText(artistName);
            } else {
                Dbg.syserr("Artist container null");
                this.nowPlaylingArtist.setText("  ");
            }
        } else {
            this.nowPlayingTrack.setText("No Song Playing");
            this.nowPlaylingArtist.setText("  ");
            Marquee.marqueeText = "No Song Playing";
        }

    }

    //Remove from now playing. If this is the current track, then return true so that the playback can be stopped.
    public boolean removeTrack(int row) {
        tracks.remove(row);
        if (row == position) {
            return true;
        }
        return false;
    }

    //Remove from now playing. If this is the current track, then return true so that the playback can be stopped.
    public boolean removeTrack(int[] rows) {
        Arrays.sort(rows);
        int removed = 0;
        boolean currTrackRemoved = false;
        for (int i : rows) {
            tracks.remove(i - removed);
            removed++;
            if (i == position) {
                currTrackRemoved = true;
            } else if (i < position) {
                //Reduce position if removing tracks before the current position
                position--;
            }
        }
        //position -= removed - 1; //Take off one less to play the next track.
        return currTrackRemoved;
    }

    public void clearTracks() {
        tracks = new ArrayList<Track>();
        position = 0;
    }

    public void moveTracks(int[] indexesToMove, int row) {
        //System.out.println("Moving tracks (nowplaying)");
        //System.out.println("Current position of now playing: " + position + "out of " + tracks.size() + " tracks");
        Arrays.sort(indexesToMove);
        ArrayList<Track> newTracks = new ArrayList<Track>();
        int newPosition = position;
        for (int i = 0; i <= tracks.size(); i++) {
            if (i == row) {
                //if i = row, this is the insertion point. cyle through indexesToMove and copy them across
                for (int j = 0; j < indexesToMove.length; j++) {
                    //System.out.println("Adding index : " + indexesToMove[j] + "to new list. In array");
                    newTracks.add(tracks.get(indexesToMove[j]));
                    //Check if we've moved the currently playing track. If so, change position
                    if (indexesToMove[j] == position) {
                        //	System.out.println("Setting position to(innner for loop) : " + newTracks.size());
                        newPosition = newTracks.size() - 1;
                    }
                }
            }

            //Add track.get(i) to new tracks if it's not in the list of indexes to move
            //Also check array bounds -- we have to cycle with i untill one after the length, in case we drop at the end of the list.

            if (i < tracks.size() && Arrays.binarySearch(indexesToMove, i) < 0) {
                //System.out.println("Adding index : " + i + "to new list. Not in array");
                newTracks.add(tracks.get(i));
                //Check if we've moved the currently playing track. If so, change position
                if (i == position) {
                    //	System.out.println("Setting position to : " + newTracks.size());
                    newPosition = newTracks.size() - 1;
                }
            }

        }
        //Sanity check
        if (tracks.size() != newTracks.size()) {
            Dbg.syserr("Mismatched sizes in move. Old: " + tracks.size() + ". New: " + newTracks.size());
        } else {
            //Copy newTracks to old.
            tracks = newTracks;
            //Set postion
            position = newPosition;
        }
        //System.out.println("New position of now playing: " + position + "out of " + tracks.size() + " tracks");
    }

    public boolean isPlaying(Track track) {
        try {
            if (tracks.get(position).getTrackID() == track.getTrackID() && Track.TracksAreSame(track, tracks.get(position))&& isPlaying) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public boolean isPlaying(int positionInPlaylist) {
        return (positionInPlaylist == position && isPlaying);
    }

    public void playBackFinished() {
        isPlaying = false;
        position = 0;
        updateText();
    }

    public void playBackStarted() {
        isPlaying = true;
    }
}
