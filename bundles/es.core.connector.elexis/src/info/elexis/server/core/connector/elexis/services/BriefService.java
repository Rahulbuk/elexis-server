package info.elexis.server.core.connector.elexis.services;

import java.util.Optional;

import info.elexis.server.core.connector.elexis.jpa.model.annotated.Brief;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Heap;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Kontakt;

public class BriefService extends PersistenceService {

	public static class Builder extends AbstractBuilder<Brief> {

		private Heap heap;

		/**
		 * Requires a {@link Heap} object with corresponding id, use
		 * {@link #buildAndSave()} to ensure proper creation.
		 * 
		 * @param patient
		 */
		public Builder(Kontakt patient) {
			object = new Brief();
			object.setPatient(patient);

			heap = new Heap();
			heap.setId(object.getId());
			object.setContent(heap);
			object.setGeloescht(false);
		}

		@Override
		public Brief buildAndSave() {
			PersistenceService.save(heap);
			return super.buildAndSave();
		}
	}

	/**
	 * convenience method
	 * 
	 * @param id
	 * @return
	 */
	public static Optional<Brief> load(String id) {
		return PersistenceService.load(Brief.class, id).map(v -> (Brief) v);
	}

}
