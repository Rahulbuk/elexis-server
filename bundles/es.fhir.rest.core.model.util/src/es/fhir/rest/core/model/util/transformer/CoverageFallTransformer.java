package es.fhir.rest.core.model.util.transformer;

import java.time.LocalDate;
import java.util.Optional;

import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Period;
import org.osgi.service.component.annotations.Component;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.primitive.IdDt;
import ch.elexis.core.model.FallConstants;
import es.fhir.rest.core.IFhirTransformer;
import es.fhir.rest.core.model.util.transformer.helper.AbstractHelper;
import es.fhir.rest.core.model.util.transformer.helper.FallHelper;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Fall;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Kontakt;
import info.elexis.server.core.connector.elexis.services.FallService;
import info.elexis.server.core.connector.elexis.services.KontaktService;

@Component
public class CoverageFallTransformer implements IFhirTransformer<Coverage, Fall> {

	private FallHelper fallHelper = new FallHelper();

	@Override
	public Optional<Coverage> getFhirObject(Fall localObject) {
		Coverage coverage = new Coverage();

		coverage.setId(new IdDt("Coverage", localObject.getId()));
		coverage.addIdentifier(getElexisObjectIdentifier(localObject));

		coverage.setBin(fallHelper.getBin(localObject));
		coverage.setBeneficiary(fallHelper.getBeneficiaryReference(localObject));
		coverage.setIssuer(fallHelper.getIssuerReference(localObject));
		coverage.setPeriod(fallHelper.getPeriod(localObject));

		fallHelper.getType(localObject).ifPresent(coding -> {
			coverage.setType(coding);
		});

		fallHelper.setText(coverage, fallHelper.getFallText(localObject));

		return Optional.of(coverage);
	}

	@Override
	public Optional<Fall> getLocalObject(Coverage fhirObject) {
		if (fhirObject != null && fhirObject.getId() != null) {
			Optional<Fall> existing = FallService.load(fhirObject.getId());
			if (existing.isPresent()) {
				return Optional.of((Fall) existing.get());
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Fall> updateLocalObject(Coverage fhirObject, Fall localObject) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<Fall> createLocalObject(Coverage fhirObject) {
		if (fhirObject.hasBeneficiaryReference()) {
			try {
				Optional<Kontakt> patient = KontaktService
						.load(fhirObject.getBeneficiaryReference().getReferenceElement().getIdPart());
				Optional<String> type = fallHelper.getType(fhirObject);
				if (patient.isPresent() && type.isPresent()) {
					Fall created = new FallService.Builder(patient.get(), "online created", FallConstants.TYPE_DISEASE,
							type.get()).buildAndSave();
					String bin = fhirObject.getBin();
					if (bin != null) {
						fallHelper.setBin(created, bin);
					}
					Period period = fhirObject.getPeriod();
					if (period != null && period.getStart() != null) {
						fallHelper.setPeriod(created, fhirObject.getPeriod());
					} else {
						created.setDatumVon(LocalDate.now());
					}
					created = (Fall) FallService.save(created);
					AbstractHelper.acquireAndReleaseLock(created);
					return Optional.of(created);
				} else {
					LoggerFactory.getLogger(CoverageFallTransformer.class)
							.warn("Could not create fall for patinet [" + patient + "] type [" + type + "]");
				}
			} catch (FHIRException e) {
				LoggerFactory.getLogger(CoverageFallTransformer.class).error("Could not create local object.", e);
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean matchesTypes(Class<?> fhirClazz, Class<?> localClazz) {
		return Coverage.class.equals(fhirClazz) && Fall.class.equals(localClazz);
	}

}
