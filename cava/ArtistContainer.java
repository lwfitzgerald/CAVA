package cava;

import java.util.Hashtable;

public class ArtistContainer {
	Hashtable<Integer, Integer> hashRowNumToID;
	Artist[] artists=null;
	TrackDatabase db;
	int numArtists;
	
	public ArtistContainer() {
		db = new TrackDatabase();
		if(db.isConnected()){
			hashRowNumToID = new Hashtable<Integer, Integer>();
		}
	}
	
	public void updateArtists(){
		hashRowNumToID.clear();
		setArtists();
		//setNumArtists(null);
	}
	
	
	public void getArtistsWithIDs(Integer[] artistIDs){
		hashRowNumToID.clear();
		artists = db.getArtistsWithIDs(artistIDs);
		numArtists = db.getNumArtistsWithIDs(artistIDs);
		if(artists!=null){
			int i = 0;
			for(Artist artist: artists){
				hashRowNumToID.put(artist.getArtistID(),i++);
			}
		}
	}
	
	private void setArtists(){
		artists = db.getAllArtists();
		if(artists!=null){
			int i = 0;
			for(Artist artist: artists){
				hashRowNumToID.put(artist.getArtistID(),i++);
			}
			numArtists = i;
		}else{
			numArtists = 0;
		}
	}
	
	
	public int getNumArtists(){
		return this.numArtists;
	}
	
	public Artist getArtistByRowNumber(int row){
		try{
			return artists[row];
		}catch (Exception e) {
			return null;
		}
	}
	
	public Artist getArtistByID(int artistID){
		Integer row = hashRowNumToID.get(artistID);
		if(row==null){
			return null;
		}else{
			try{
				return artists[row];
			}catch (Exception e) {
				return null;
			}
		}
	}
	
	public int getRowNumFromID(int artistID){
		Integer row =  hashRowNumToID.get(artistID);
		if(row==null){
			return -1;
		}else{
			return row;
		}
	}
	
	public int getFirstRowMatching(String query){
		return getFirstRowMatching(query, 0, artists.length);
	}
	
	public int getFirstRowMatching(String query,int start, int end){
		if(start >= end){
			return -1;
		}
		int mid = Math.round((end+start)/2);
		if(mid==start){
			return -1;
		}
		//System.out.println("Artist at mid: " + BuildDatabase.createSortByArtist(artists[mid].getArtistName()));
		if(BuildDatabase.createSortByArtist(artists[mid].getArtistName()).startsWith(query)){
			//Loop until it doesn't start with the query string
			while(mid >= 0 && BuildDatabase.createSortByArtist(artists[mid].getArtistName()).startsWith(query)){
				mid--;
			}
			//plus one more to ignore all, plus one to get first matching row
			return mid+2;
		}else{
			if(BuildDatabase.createSortByArtist(artists[mid].getArtistName()).compareTo(query) < 0){
				//System.out.println("Compared to:" + artists[mid].getArtistName().substring(0, query.length()));
				return getFirstRowMatching(query, mid, end);
			}else{
				return getFirstRowMatching(query,start,mid);
			}
		}
		
	}
	
}
