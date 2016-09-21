package es.fhir.rest.core.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.uhn.fhir.rest.client.IGenericClient;
import es.fhir.rest.core.test.AllTests;
import es.fhir.rest.core.test.FhirClient;
import info.elexis.server.core.connector.elexis.jpa.test.TestDatabaseInitializer;

public class EncounterTest {

	private static IGenericClient client;

	@BeforeClass
	public static void setupClass() {
		TestDatabaseInitializer initializer = new TestDatabaseInitializer();
		initializer.initializeBehandlung();

		client = FhirClient.getTestClient();
		assertNotNull(client);

	}

	@Test
	public void getEncounter() {
		Patient readPatient = client.read().resource(Patient.class).withId(TestDatabaseInitializer.getPatient().getId())
				.execute();
		// search by patient
		Bundle results = client.search().forResource(Encounter.class)
				.where(Encounter.PATIENT.hasId(readPatient.getId())).returnBundle(Bundle.class).execute();
		assertNotNull(results);
		List<BundleEntryComponent> entries = results.getEntry();
		assertFalse(entries.isEmpty());
		Encounter encounter = (Encounter) entries.get(0).getResource();

		// search by elexis behandlung id
		results = client.search().forResource(Encounter.class)
				.where(Encounter.IDENTIFIER.exactly().systemAndIdentifier("www.elexis.info/consultationid",
						TestDatabaseInitializer.getBehandlung().getId()))
				.returnBundle(Bundle.class).execute();
		entries = results.getEntry();
		assertFalse(entries.isEmpty());

		// read with by id
		Encounter readEncounter = client.read().resource(Encounter.class).withId(encounter.getId()).execute();
		assertNotNull(readEncounter);
		assertEquals(encounter.getId(), readEncounter.getId());
	}

	/**
	 * Test all properties set by
	 * {@link TestDatabaseInitializer#initializeBehandlung()}.
	 */
	@Test
	public void getEncounterProperties() {
		Bundle results = client.search().forResource(Encounter.class).where(Encounter.IDENTIFIER.exactly()
				.systemAndIdentifier("www.elexis.info/consultationid", TestDatabaseInitializer.getBehandlung().getId()))
				.returnBundle(Bundle.class).execute();
		List<BundleEntryComponent> entries = results.getEntry();
		assertFalse(entries.isEmpty());
		Encounter encounter = (Encounter) entries.get(0).getResource();

		assertEquals("Practitioner/" + TestDatabaseInitializer.getMandant().getId(),
				encounter.getServiceProvider().getReference());
		assertEquals("Patient/" + TestDatabaseInitializer.getPatient().getId(), encounter.getPatient().getReference());
		Period period = encounter.getPeriod();
		assertNotNull(period);
		assertEquals(LocalDate.of(2016, Month.SEPTEMBER, 21),
				AllTests.getLocalDateTime(period.getStart()).toLocalDate());
		Narrative narrative = encounter.getText();
		assertNotNull(narrative);
		String text = narrative.getDivAsString();
		assertNotNull(text);
		assertTrue(text.contains("Test consultation"));
	}
}
