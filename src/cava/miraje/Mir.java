package cava.miraje;

import java.util.*;
import cava.*;
import cava.server.ServerDatabase;
import cava.server.ServerTrack;

public class Mir {
    /**
     * Sampling rate
     */
    private static final int samplingrate = 22050;
    
    /**
     * Window size
     */
    private static final int windowsize = 1024;
    
    /**
     * Mel coefficients
     */
    private static final int melcoefficients = 36;
    
    /**
     * Mfcc coefficients
     */
    private static final int mfcccoefficients = 20;
    
    /**
     * Seconds to analyse
     */
    private static final int secondstoanalyse = 120;

    /**
     * Mfcc object to generate Mfcc matrices
     */
    private static Mfcc mfcc = new Mfcc(windowsize, samplingrate, melcoefficients, mfcccoefficients);

    /**
     * Audio Decoder
     */
    private AudioDecoder ad = new AudioDecoder(samplingrate, secondstoanalyse, windowsize);

    /**
     * Cancel current analysis
     */
    public void cancelAnalyse() {
        ad.cancelDecode();
    }

    /**
     * Analyse an audio file and generate the Scms for it
     * 
     * @param file
     *            Path to audio file
     * @return Scms object for audio file
     * @throws MirAnalysisImpossibleException
     *             Thrown if analysis fails at any stage
     */
    public Scms analyse(String file) throws MirAnalysisImpossibleException {
        try {
            DbgTimer t = new DbgTimer();
            cava.Dbg.sysout("Analysing file: "+file);
            Matrix stftdata = ad.decode(file);
            Matrix mfccdata = mfcc.apply(stftdata);
            Scms scms = Scms.getScms(mfccdata);

            Dbg.println("MiraJe: Total execution time: " + t.stop() + "ms");

            return scms;
        } catch (Exception e) {
            throw new MirAnalysisImpossibleException();
        }
    }

    /**
     * Get IDs of similar tracks to those specified
     * 
     * @param tracks
     *            Seed tracks
     * @param db
     *            Track Database
     * @param length
     *            Number of track IDs to return
     * @return Array of track ID's
     */
    public static int[] similarTracks(ClientTrack[] tracks, TrackDatabase db, int length) {
        return similarTracks(tracks, db, length, 0.20f);
    }

    /**
     * Get IDs of similar tracks to those specified
     * 
     * @param tracks
     *            Seed tracks
     * @param db
     *            Track Database
     * @param length
     *            Number of track IDs to return
     * @param distceiling
     *            Minimum distance from seed tracks
     * @return Array of track ID's
     */
    public static int[] similarTracks(ClientTrack[] seeds, TrackDatabase db, int length, float distceiling) {
        DbgTimer t = new DbgTimer();

        // Get Seed-Song SCMS
        Scms[] seedScms = new Scms[seeds.length];
        for(int i=0; i < seedScms.length; i++) {
        	if(seeds[i] == null){
        		continue;
        	}
        	
            seedScms[i] = new Scms(mfcccoefficients);
            Scms.fromBytes(seeds[i].getScms(), seedScms[i]);
        }

        // Create Scms objects for the tracks to load their byte arrays into
        Scms[] scmss = new Scms[5000];
        for(int i=0; i < 5000; i++) {
            scmss[i] = new Scms(mfcccoefficients);
        }

        // Allocate the Scms Distance cache
        ScmsConfiguration c = new ScmsConfiguration(mfcccoefficients);
        
        // Create LinkedList
        LinkedList<DistanceHolder> distancelist = new LinkedList<DistanceHolder>();
        
        ClientTrack[] tracks = db.getLimitedTracks(1, 5000, seeds);
        
        //Make sure some tracks were actually returned. Otherwise, return an empty array
        if(tracks==null || tracks.length==0){
        	return new int[0];
        }

        for(int index = 1; tracks != null; index++) {
            // Load the Scms objects from the bytes in each Track object
            for(int i=0; i < tracks.length; i++) {
                Scms.fromBytes(tracks[i].getScms(), scmss[i]);
            }
            
            for(int i=0; i < tracks.length; i++) {
                float d = 0;
                int count = 0;
                for(int j=0; j < seedScms.length; j++) {
                	if(seeds[j] == null){
                		continue;
                	}
                    float dcur = Scms.distance(seedScms[j], scmss[i], c);

                    // Possible negative numbers indicate faulty Scms models..
                    if(dcur >= 0) {
                        d += dcur;
                        count++;
                    } else {
                        Dbg.println("MiraJe: Faulty SCMS id=" + tracks[i].getPath(db) + " d=" + d);
                        d = Float.MAX_VALUE;
                        break;
                    }
                }

                // Exclude track if it's too close to the seeds
                if(d > distceiling) {
                    distancelist.addLast(new DistanceHolder(tracks[i], d/count));
                } else {
                    Dbg.println("MiraJe: ignoring " + tracks[i].getPath(db) + "d: " + d);
                }
            }
            
            tracks = db.getLimitedTracks(index * 5000 + 1, (index+1) * 5000, seeds);
            System.gc();
        }
        
        int[] playlist = getBestMatchesInt(distancelist, length, true);
        
        Dbg.println("MiraJe: Generated playlist in: " + t.stop() + "ms");
        
        return playlist;
    }
    
    /**
     * Converts the given distance list to an array and sorts it by distance
     * 
     * @param distancelist
     *            Distance list
     * @return Array sorted by distance
     */
    private static DistanceHolder[] prepareBestMatches(LinkedList<DistanceHolder> distancelist) {
        // Copy LinkedList into array
        DistanceHolder[] distancearray = new DistanceHolder[distancelist.size()];
        distancelist.toArray(distancearray);
        
        // Sort the array by distance
        Arrays.sort(distancearray, new Comparator<DistanceHolder>() {
            public int compare(DistanceHolder arg0, DistanceHolder arg1) {
                if(arg0.distance < arg1.distance) {
                    return -1;
                } else if(arg0.distance > arg1.distance) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        
        return distancearray;
    }
    
    /**
     * Returns the specified number of best matches in the given distance list
     * 
     * @param distancelist
     *            Distance list
     * @param numtracks
     *            Number of best matches required
     * @param trackid
     *            If true, return an array of track IDs. If false return an
     *            array of group IDs
     * @return Array of IDs
     */
    private static int[] getBestMatchesInt(LinkedList<DistanceHolder> distancelist, int numtracks, boolean trackid) {
        DistanceHolder[] distancearray = prepareBestMatches(distancelist);
        numtracks = Math.min(numtracks, distancearray.length);
        
        int[] playlist = new int[numtracks];
        
        for(int i=0; i < numtracks; i++) {
            if(trackid) {
                playlist[i] = distancearray[i].track.getTrackID();
            } else {
                playlist[i] = ((cava.server.ServerTrack) distancearray[i].track).getScmsGroup();
            }
        }
        
        return playlist;
    }
    
    /**
     * Returns the specified number of best matches in the given distance list
     * as Track objects
     * 
     * @param distancelist
     *            Distance list
     * @param numtracks
     *            Number of best matches required
     * @param trackid
     *            If true, return an array of track IDs. If false return an
     *            array of group IDs
     * @return Array of Track object
     */
    private static cava.server.ServerTrack[] getBestMatchesTrack(LinkedList<DistanceHolder> distancelist, int numtracks) {
        DistanceHolder[] distancearray = prepareBestMatches(distancelist);
        numtracks = Math.min(numtracks, distancearray.length);
        
        cava.server.ServerTrack[] playlist = new cava.server.ServerTrack[numtracks];
        
        for(int i=0; i < numtracks; i++) {
            playlist[i] = (cava.server.ServerTrack) distancearray[i].track;
        }
        
        return playlist;
    }
    
    /**
     * Do a fast search in the uploaded table for the Scms group id of the given
     * track
     * 
     * @param track
     *            Track
     * @param seed
     *            Scms for Track
     * @param c
     *            Scms distance configuration cache
     * @param db
     *            Server database
     * @param distceiling
     *            Distance below which tracks are classed as the same
     * @return Scms group id
     */
    private static Integer fastSearchUploaded(cava.server.ServerTrack track, Scms seed, ScmsConfiguration c, ServerDatabase db, float distceiling) {
        cava.server.ServerTrack[] matches = db.getTracksFromUploads(track);
        return fastSearch(track, seed, matches, c, db, distceiling);
    }
    
    /**
     * Do a fast search in the aliases table for the Scms group id of the given
     * track
     * 
     * @param track
     *            Track
     * @param seed
     *            Scms for Track
     * @param c
     *            Scms distance configuration cache
     * @param db
     *            Server database
     * @param distceiling
     *            Distance below which tracks are classed as the same
     * @return Scms group id
     */
    private static Integer fastSearchAliases(cava.server.ServerTrack track, Scms seed, ScmsConfiguration c, ServerDatabase db, float distceiling) {
        cava.server.ServerTrack[] matches = db.getTracksFromAliases(track);
        return fastSearch(track, seed, matches, c, db, distceiling);
    }
    
    /**
     * Do a fast search in the given matches for the Scms group id of the given
     * track
     * 
     * @param track
     *            Track
     * @param seed
     *            Scms for Track
     * @param matches
     *            Tracks matching artist name and track name
     * @param c
     *            Scms distance configuration cache
     * @param db
     *            Server database
     * @param distceiling
     *            Distance below which tracks are classed as the same
     * @return Scms group id
     */
    private static Integer fastSearch(cava.server.ServerTrack track, Scms seed, cava.server.ServerTrack[] matches, ScmsConfiguration c, ServerDatabase db, float distceiling) {
        if(matches != null) {
            LinkedList<DistanceHolder> distancelist = new LinkedList<DistanceHolder>();
            
            Scms matchScms = new Scms(mfcccoefficients);
            
            for(int i=0; i < matches.length; i++) {
                float d;
                
                Scms.fromBytes(matches[i].getScms(), matchScms);
                
                if((d = Scms.distance(seed, matchScms, c)) <= distceiling) {
                    distancelist.addLast(new DistanceHolder(matches[i], d));
                }
            }
            
            // Return the best match if there is one
            if(!distancelist.isEmpty()) {
                return getBestMatchesInt(distancelist, 1, false)[0];
            }
        }
        
        return null;
    }
    
    /**
     * Do a slow search in the uploaded table for the Scms group id of the given
     * track
     * 
     * @param track
     *            Track
     * @param seed
     *            Scms for Track
     * @param c
     *            Scms distance configuration cache
     * @param db
     *            Server database
     * @param distceiling
     *            Distance below which tracks are classed as the same
     * @return Scms group id
     */
    private static Integer slowSearchUploaded(cava.server.ServerTrack track, Scms seed, ScmsConfiguration c, ServerDatabase db, float distceiling) {
        LinkedList<DistanceHolder> distancelist = new LinkedList<DistanceHolder>();
        
        // Create an array containing the single seed track
        cava.server.ServerTrack[] seedarray = { track };
        
        // Create Scms objects for the tracks to load their byte arrays into
        Scms[] scmss = new Scms[5000];
        for(int i=0; i < 5000; i++) {
            scmss[i] = new Scms(mfcccoefficients);
        }
        
        cava.server.ServerTrack[] tracks = db.getLimitedUploadedTracks(1, 5000, seedarray);

        for(int index = 1; tracks != null; index++) {
            // Load the Scms objects from the bytes in each Track object
            for(int i=0; i < tracks.length; i++) {
                Scms.fromBytes(tracks[i].getScms(), scmss[i]);
            }
            
            for(int i=0; i < tracks.length; i++) {
                float d = Scms.distance(seed, scmss[i], c);
                
                if(d <= distceiling) {
                    distancelist.addLast(new DistanceHolder(tracks[i], d));
                }
            }
            
            tracks = db.getLimitedUploadedTracks(index * 5000 + 1, (index+1) * 5000, seedarray);
            System.gc();
        }
        
        if(!distancelist.isEmpty()) {
            return getBestMatchesInt(distancelist, 1, false)[0];
        } else {
            return null;
        }
    }
    
    /**
     * Searches the Server Database for existing entries of a given track 
     * @param track
     * @param db
     * @param distceiling
     * @return
     */
    public static Integer getGroupID(cava.server.ServerTrack track, ServerDatabase db, float distceiling) {
        // Load the Scms for the seed track
        Scms seed = new Scms(mfcccoefficients);
        Scms.fromBytes(track.getScms(), seed);
        
        // Allocate the Scms Distance cache
        ScmsConfiguration c = new ScmsConfiguration(mfcccoefficients);
        
        // First, do a fast search on uploaded but not available tracks
        Integer result;
        DbgTimer t = new DbgTimer();
        if((result = fastSearchUploaded(track, seed, c, db, distceiling)) != null) {
            Dbg.println("MiraJe: Fast Search Uploaded succeeded in " + t.stop() + "ms");
            return result;
        }
        
        // Second, do a fast search on aliases
        t.reset();
        if((result = fastSearchAliases(track, seed, c, db, distceiling)) != null) {
            Dbg.println("MiraJe: Fast Search Aliases succeeded in " + t.stop() + "ms");
            return result;
        }
        
        // Finally, do a slow search on uploaded but not available tracks
        t.reset();
        if((result = slowSearchUploaded(track, seed, c, db, distceiling)) != null) {
            Dbg.println("MiraJe: Slow Search Uploaded succeeded in " + t.stop() + "ms");
            return result;
        } else {
            Dbg.println("MiraJe: Search for existing entries found no results (took " + t.stop() + "ms)");
            return null;
        }
    }
    
    /**
     * Finds similar tracks in the Server's database
     * 
     * @param seeds
     *            Seed tracks
     * @param db
     *            Server database
     * @param length
     *            Number of tracks to return
     * @param distceiling
     * @return Minimum distance from seed tracks
     */
    public static cava.server.ServerTrack[] serverSimilarTracks(cava.server.ServerTrack seeds[], ServerDatabase db, int length, float distceiling) {
        DbgTimer t = new DbgTimer();

        // Get Seed-Song SCMS
        Scms[] seedScms = new Scms[seeds.length];
        for(int i=0; i < seedScms.length; i++) {
            if(seeds[i] == null){
                continue;
            }
            
            seedScms[i] = new Scms(mfcccoefficients);
            Scms.fromBytes(seeds[i].getScms(), seedScms[i]);
        }

        // Create Scms objects for the tracks to load their byte arrays into
        Scms[] scmss = new Scms[5000];
        for(int i=0; i < 5000; i++) {
            scmss[i] = new Scms(mfcccoefficients);
        }

        // Allocate the Scms Distance cache
        ScmsConfiguration c = new ScmsConfiguration(mfcccoefficients);
        
        // Create LinkedList
        LinkedList<DistanceHolder> distancelist = new LinkedList<DistanceHolder>();
        
        Track[] tracks = db.getLimitedAvailableTracks(1, 5000, seeds);

        for(int index = 1; tracks != null; index++) {
            // Load the Scms objects from the bytes in each Track object
            for(int i=0; i < tracks.length; i++) {
                Scms.fromBytes(tracks[i].getScms(), scmss[i]);
            }
            
            for(int i=0; i < tracks.length; i++) {
                float d = 0;
                int count = 0;
                for(int j=0; j < seedScms.length; j++) {
                    if(seeds[j] == null){
                        continue;
                    }
                    float dcur = Scms.distance(seedScms[j], scmss[i], c);

                    // Possible negative numbers indicate faulty Scms models..
                    if(dcur >= 0) {
                        d += dcur;
                        count++;
                    } else {
                        Dbg.println("MiraJe: Faulty SCMS id=" + ((ClientTrack)tracks[i]).getPath(db) + " d=" + d);
                        d = Float.MAX_VALUE;
                        break;
                    }
                }

                // Exclude track if it's too close to the seeds
                if(d > distceiling) {
                    distancelist.addLast(new DistanceHolder(tracks[i], d/count));
                } else {
                    Dbg.print("MiraJe: ignoring ");
                    if(tracks[i].isClientTrack()){
                    	Dbg.print(((ClientTrack)tracks[i]).getPath(db));
                    }else{
                    	Dbg.print(((ServerTrack)tracks[i]).getTrackName());
                    }
                    Dbg.println(" d: " + d);
                }
            }
            
            tracks = db.getLimitedAvailableTracks(index * 5000 + 1, (index+1) * 5000, seeds);
            System.gc();
        }
        
        cava.server.ServerTrack[] playlist = (cava.server.ServerTrack[]) getBestMatchesTrack(distancelist, length);
        
        Dbg.println("MiraJe: Server generated playlist in: " + t.stop() + "ms");
        
        return (cava.server.ServerTrack[]) playlist;
    }
    
    /**
     * Internal class to holds a track and it's distance from the seed tracks
     */
    private static class DistanceHolder {
        /**
         * Track
         */
        public final Track track;
        
        /**
         * Distance from seed tracks
         */
        public final float distance;
        
        /**
         * Create a holder for the given track ID and distance
         * 
         * @param trackid
         *            Track ID
         * @param distance
         *            Distance from seed tracks
         */
        public DistanceHolder(Track track, float distance) {
            this.track = track;
            this.distance = distance;
        }
    }
}
