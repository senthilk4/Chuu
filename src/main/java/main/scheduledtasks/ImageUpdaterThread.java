package main.scheduledtasks;

import dao.DaoImplementation;
import dao.entities.ArtistInfo;
import main.Chuu;
import main.apis.discogs.DiscogsApi;
import main.apis.discogs.DiscogsSingleton;
import main.exceptions.DiscogsServiceException;

import java.util.Set;

public class ImageUpdaterThread implements Runnable {
	private final DaoImplementation dao;
	private final DiscogsApi discogsApi;

	public ImageUpdaterThread(DaoImplementation dao) {
		this.dao = dao;
		this.discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();

	}

	@Override
	public void run() {
		Set<String> artistData = dao.getNullUrls();
		System.out.println("Found at lest " + artistData.size() + "null artist ");
		for (String artistDatum : artistData) {
			String url;
			System.out.println("Working with artist " + artistDatum);
			try {
				Thread.sleep(1000);
				url = discogsApi.findArtistImage(artistDatum);
				if (url != null) {

					System.out.println("Upserting buddy");
					if (!url.isEmpty())
						System.out.println(artistDatum);
					dao.upsertUrl(new ArtistInfo(url, artistDatum));
				}
			} catch (DiscogsServiceException e) {
				Chuu.getLogger().warn(e.getMessage(), e);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}


	}


}