package mr.supnum.bib.impl;

import com.sun.xml.ws.developer.SchemaValidation;
import jakarta.jws.WebService;
import java.time.LocalDate;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import mr.supnum.bib.contrat.EmpruntPortType;
import mr.supnum.bib.contrat.EmpruntType;
import mr.supnum.bib.contrat.LivreIndisponible;
import mr.supnum.bib.contrat.LivreIndisponibleFault;

/**
 * Implémentation contract-first : l'interface {@link EmpruntPortType} est générée
 * par wsimport depuis {@code EmpruntService.wsdl}, qui reste la source de vérité.
 * {@link SchemaValidation} fait valider chaque message contre le schéma du contrat.
 */
@SchemaValidation
@WebService(serviceName = "EmpruntService", portName = "EmpruntPort",
            targetNamespace = "http://supnum.mr/bib",
            endpointInterface = "mr.supnum.bib.contrat.EmpruntPortType",
            wsdlLocation = "wsdl/EmpruntService.wsdl")
public class EmpruntServiceImpl implements EmpruntPortType {

    private static final String ISBN_SORTI = "978-2-10-074444-4";
    private static final LocalDate RETOUR_PREVU = LocalDate.of(2026, 12, 2);
    private static final int DUREE_EMPRUNT_JOURS = 14;

    private static final DatatypeFactory DATATYPES = creerDatatypeFactory();

    @Override
    public EmpruntType emprunterLivre(String idAdherent, String isbn) throws LivreIndisponibleFault {
        if (ISBN_SORTI.equals(isbn)) {
            LivreIndisponible info = new LivreIndisponible();
            info.setIsbn(isbn);
            info.setRetourPrevu(date(RETOUR_PREVU));
            throw new LivreIndisponibleFault("Livre indisponible", info);
        }
        EmpruntType emprunt = new EmpruntType();
        emprunt.setNumero("E-2026-0812");
        emprunt.setDateRetour(date(LocalDate.now().plusDays(DUREE_EMPRUNT_JOURS)));
        return emprunt;
    }

    private static XMLGregorianCalendar date(LocalDate jour) {
        return DATATYPES.newXMLGregorianCalendar(jour.toString());
    }

    private static DatatypeFactory creerDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("DatatypeFactory indisponible", e);
        }
    }
}
