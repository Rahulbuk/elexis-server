package es.fhir.rest.core.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.valuesets.ConditionCategory;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ch.elexis.core.findings.util.ModelUtil;
import es.fhir.rest.core.test.AllTests;
import info.elexis.server.core.connector.elexis.jpa.test.TestDatabaseInitializer;

public class EncounterTest {

	private static IGenericClient client;

	@BeforeClass
	public static void setupClass() throws IOException, SQLException {
		TestDatabaseInitializer initializer = new TestDatabaseInitializer();
		initializer.initializeBehandlung();
		initializer.initializeMandant();

		client = ModelUtil.getGenericClient("http://localhost:8380/fhir");
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

		// search by patient and date
		results = client.search().forResource(Encounter.class).where(Encounter.PATIENT.hasId(readPatient.getId()))
				.and(Encounter.DATE.afterOrEquals().day("2016-09-01"))
				.and(Encounter.DATE.beforeOrEquals().day("2016-10-01")).returnBundle(Bundle.class).execute();
		assertNotNull(results);
		entries = results.getEntry();
		assertFalse(entries.isEmpty());

		// search by patient and date not found
		results = client.search().forResource(Encounter.class).where(Encounter.PATIENT.hasId(readPatient.getId()))
				.and(Encounter.DATE.afterOrEquals().day("2016-10-01"))
				.and(Encounter.DATE.beforeOrEquals().day("2016-11-01")).returnBundle(Bundle.class).execute();
		assertNotNull(results);
		entries = results.getEntry();
		assertTrue(entries.isEmpty());
	}

	@Test
	public void createEncounter() {
		Condition subjective = new Condition();
		Narrative narrative = new Narrative();
		String divEncodedText = "Subjective\nTest".replaceAll("(\r\n|\r|\n)", "<br />");
		narrative.setDivAsString(divEncodedText);
		subjective.setText(narrative);
		subjective.setSubject(new Reference("Patient/" + TestDatabaseInitializer.getPatient().getId()));
		subjective.setCategory(new CodeableConcept().addCoding(new Coding(ConditionCategory.COMPLAINT.getSystem(),
				ConditionCategory.COMPLAINT.toCode(), ConditionCategory.COMPLAINT.getDisplay())));
		MethodOutcome subjectiveOutcome = client.create().resource(subjective).execute();

		Condition problem = new Condition();
		problem.setCode(
				new CodeableConcept().addCoding(new Coding("http://hl7.org/fhir/sid/icpc-2", "A04", "Müdigkeit")));
		problem.setSubject(new Reference("Patient/" + TestDatabaseInitializer.getPatient().getId()));
		problem.setCategory(new CodeableConcept().addCoding(new Coding(ConditionCategory.DIAGNOSIS.getSystem(),
				ConditionCategory.DIAGNOSIS.toCode(), ConditionCategory.DIAGNOSIS.getDisplay())));
		MethodOutcome problemOutcome = client.create().resource(problem).execute();

		Encounter encounter = new Encounter();
		EncounterParticipantComponent participant = new EncounterParticipantComponent();
		participant.setIndividual(new Reference("Practitioner/" + TestDatabaseInitializer.getMandant().getId()));
		encounter.addParticipant(participant);
		encounter.setPeriod(new Period().setStart(AllTests.getDate(LocalDate.now().atStartOfDay()))
				.setEnd(AllTests.getDate(LocalDate.now().atTime(23, 59, 59))));
		encounter.setPatient(new Reference("Patient/" + TestDatabaseInitializer.getPatient().getId()));

		encounter.addIndication(
				new Reference(new IdType(subjective.getResourceType().name(), subjectiveOutcome.getId().getIdPart())));
		encounter.addIndication(
				new Reference(new IdType(problem.getResourceType().name(), problemOutcome.getId().getIdPart())));

		encounter.addType(new CodeableConcept()
				.addCoding(new Coding("www.elexis.info/encounter/type", "struct", "structured enconter")));

		MethodOutcome outcome = client.create().resource(encounter).execute();
		assertNotNull(outcome);
		assertTrue(outcome.getCreated());
		assertNotNull(outcome.getId());

		Encounter readEncounter = client.read().resource(Encounter.class).withId(outcome.getId()).execute();
		assertNotNull(readEncounter);
		assertEquals(outcome.getId().getIdPart(), readEncounter.getIdElement().getIdPart());
		assertEquals(encounter.getPeriod().getStart(), readEncounter.getPeriod().getStart());
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

		assertEquals(TestDatabaseInitializer.getMandant().getId(), getMandatorId(encounter));
		assertEquals("Patient/" + TestDatabaseInitializer.getPatient().getId(), encounter.getPatient().getReference());
		List<CodeableConcept> typeConcepts = encounter.getType();
		assertNotNull(typeConcepts);
		assertFalse(typeConcepts.isEmpty());
		assertTrue(AllTests.isCodeInConcepts("www.elexis.info/encounter/type", "text", typeConcepts));
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

	private String getMandatorId(Encounter encounter) {
		List<EncounterParticipantComponent> participants = encounter.getParticipant();
		for (EncounterParticipantComponent encounterParticipantComponent : participants) {
			if (encounterParticipantComponent.hasIndividual()) {
				Reference reference = encounterParticipantComponent.getIndividual();
				if (reference.getReferenceElement().getResourceType().equals(Practitioner.class.getSimpleName())) {
					return reference.getReferenceElement().getIdPart();
				}
			}
		}
		return null;
	}
}
