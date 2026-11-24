package mr.supnum.bib;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;

/**
 * Service Emprunt, approche code-first : le WSDL est généré par JAX-WS
 * à partir de cette classe au démarrage de l'endpoint.
 */
@WebService(serviceName = "EmpruntService", portName = "EmpruntPort",
            targetNamespace = "http://supnum.mr/bib")
public class EmpruntService {

    private static final Pattern ISBN = Pattern.compile("97[89]-\\d-\\d{2,5}-\\d{2,6}-\\d");
    private static final String PREFIXE_EMPRUNT = "E-2026-";
    private static final int DUREE_EMPRUNT_JOURS = 14;

    /** Livres actuellement sortis, avec leur date de retour prévue. */
    private final Map<String, LocalDate> sortis =
            new ConcurrentHashMap<>(Map.of("978-2-10-074444-4", LocalDate.of(2026, 12, 2)));
    private final AtomicInteger compteur = new AtomicInteger(812);

    @WebMethod
    @WebResult(name = "emprunt")
    public Emprunt emprunterLivre(@WebParam(name = "idAdherent") String idAdherent,
                                  @WebParam(name = "isbn") String isbn)
            throws LivreIndisponibleException {
        if (isbn == null || !ISBN.matcher(isbn).matches()) {
            throw faultClient("ISBN mal formé : " + isbn);
        }
        LocalDate dateRetour = LocalDate.now().plusDays(DUREE_EMPRUNT_JOURS);
        // putIfAbsent rend la vérification de disponibilité et la réservation atomiques.
        LocalDate dejaSorti = sortis.putIfAbsent(isbn, dateRetour);
        if (dejaSorti != null) {
            throw new LivreIndisponibleException("Livre indisponible",
                    new LivreIndisponibleInfo(isbn, dejaSorti.toString()));
        }
        String numero = String.format("%s%04d", PREFIXE_EMPRUNT, compteur.getAndIncrement());
        return new Emprunt(numero, dateRetour.toString());
    }

    @WebMethod
    @WebResult(name = "penalite")
    public BigDecimal rendreLivre(@WebParam(name = "numeroEmprunt") String numeroEmprunt) {
        if (numeroEmprunt == null || !numeroEmprunt.startsWith(PREFIXE_EMPRUNT)) {
            throw faultClient("Numéro d'emprunt inconnu : " + numeroEmprunt);
        }
        return BigDecimal.ZERO;
    }

    /** Construit une Fault SOAP 1.1 dont le faultcode est {@code Client}. */
    private static SOAPFaultException faultClient(String message) {
        try {
            SOAPFault fault = SOAPFactory.newInstance().createFault(message,
                    new QName(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "Client"));
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new IllegalStateException("Impossible de construire la Fault SOAP", e);
        }
    }
}
