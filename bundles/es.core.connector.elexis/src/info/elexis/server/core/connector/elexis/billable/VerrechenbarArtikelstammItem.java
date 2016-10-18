package info.elexis.server.core.connector.elexis.billable;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.rgw.tools.Money;
import ch.rgw.tools.TimeTool;
import info.elexis.server.core.connector.elexis.billable.optifier.DefaultOptifier;
import info.elexis.server.core.connector.elexis.common.POHelper;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.ArtikelstammItem;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Behandlung;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Fall;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Kontakt;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Verrechnet;

public class VerrechenbarArtikelstammItem implements IBillable<ArtikelstammItem> {

	protected Logger log = LoggerFactory.getLogger(VerrechenbarArtikelstammItem.class);

	private final ArtikelstammItem artikelstammItem;

	public VerrechenbarArtikelstammItem(ArtikelstammItem artikelstammItem) {
		this.artikelstammItem = artikelstammItem;
	}

	@Override
	public String getCodeSystemName() {
		return "Artikelstamm";
	}

	@Override
	public String getCodeSystemCode() {
		String gtin = artikelstammItem.getGtin();
		if (gtin != null && gtin.length() > 3) {
			String type = artikelstammItem.getType();
			if (type != null && type.length() > 0) {
				String t = type.substring(0, 1);
				if (t.equalsIgnoreCase("P")) {
					return "402";
				} else if (t.equalsIgnoreCase("N")) {
					return "406";
				}
			}
		}
		return "999";
	}

	@Override
	public String getId() {
		return artikelstammItem.getId();
	}

	@Override
	public String getCode() {
		return artikelstammItem.getPhar();
	}

	@Override
	public String getText() {
		return artikelstammItem.getLabel();
	}

	@Override
	public List<Object> getActions(Object context) {
		return null;
	}

	@Override
	public ArtikelstammItem getEntity() {
		return artikelstammItem;
	}

	@Override
	public int getTP(TimeTool date, Fall fall) {
		int vkt = 0;
		double vpe = 0.0;
		double vke = 0.0;

		try {
			vkt = new Money(artikelstammItem.getPpub()).getCents();
		} catch (Exception e) {
			log.warn("Error parsing public price: " + e.getMessage() + " @ " + artikelstammItem.getId());
		}

		try {
			vpe = Double.parseDouble(artikelstammItem.getPkg_size());
		} catch (Exception e) {
			log.warn("Error parsing package size: " + e.getMessage() + "@ " + artikelstammItem.getId());
		}

		try {
			vke = Double.parseDouble(artikelstammItem.getVerkaufseinheit());
		} catch (Exception e) {
			log.warn("Error parsing sell unit: " + e.getMessage() + " @ " + artikelstammItem.getId());
		}

		return VerrechenbarArtikel.determineTP(date, fall, vpe, vke, vkt);
	}

	@Override
	public double getFactor(TimeTool dat, Fall fall) {
		return 1;
	}

	@Override
	public IStatus add(Behandlung kons, Kontakt userContact, Kontakt mandatorContact) {
		// if(!artikelstammItem.isSl_entry()) {
		// return new NoObligationOptifier().add(this, kons, userContact,
		// mandatorContact);
		// }
		return new DefaultOptifier().add(this, kons, userContact, mandatorContact);
	}

	@Override
	public IStatus removeFromConsultation(Verrechnet vr, Kontakt mandatorContact) {
		return new DefaultOptifier().remove(vr);
	}

	public void singleReturn(int n) {
		int anbruch = POHelper.checkZero(artikelstammItem.getAnbruch());
		int ve = POHelper.checkZero(artikelstammItem.getVerkaufseinheit());
		int vk = POHelper.checkZero(artikelstammItem.getPkg_size());
		int num = n * ve;
		if (vk == ve) {
			artikelstammItem.setIstbestand(artikelstammItem.getIstbestand() + n);
		} else {
			int rest = anbruch + num;
			while (rest > vk) {
				rest = rest - vk;
				artikelstammItem.setIstbestand(artikelstammItem.getIstbestand() + 1);
			}
			artikelstammItem.setAnbruch(Integer.toString(rest));
		}
	}

	public void singleDisposal(int n) {
		int anbruch = POHelper.checkZero(artikelstammItem.getAnbruch());
		int ve = POHelper.checkZero(artikelstammItem.getVerkaufseinheit());
		int vk = POHelper.checkZero(artikelstammItem.getPkg_size());
		if (vk == 0) {
			if (ve != 0) {
				vk = ve;
			}
		}
		if (ve == 0) {
			if (vk != 0) {
				ve = vk;
				artikelstammItem.setPkg_size(Integer.toString(ve));
			}
		}
		int num = n * ve;
		if (vk == ve) {
			int current = (artikelstammItem.getIstbestand() != null) ? artikelstammItem.getIstbestand() : 0;
			artikelstammItem.setIstbestand(current - n);
		} else {
			int rest = anbruch - num;
			while (rest < 0) {
				rest = rest + vk;
				int current = (artikelstammItem.getIstbestand() != null) ? artikelstammItem.getIstbestand() : 0;
				artikelstammItem.setIstbestand(current - 1);
			}
			artikelstammItem.setAnbruch(Integer.toString(rest));
		}
	}

	@Override
	public VatInfo getVatInfo() {
		String overridenVat = (String) artikelstammItem.getExtInfoAsString("VAT_OVERRIDE");
		if (overridenVat != null) {
			return VatInfo.valueOf(overridenVat);
		}

		switch (artikelstammItem.getType().trim()) {
		case "P":
			return VatInfo.VAT_CH_ISMEDICAMENT;
		case "N":
			return VatInfo.VAT_CH_NOTMEDICAMENT;
		}
		return VatInfo.VAT_NONE;
	}

}
