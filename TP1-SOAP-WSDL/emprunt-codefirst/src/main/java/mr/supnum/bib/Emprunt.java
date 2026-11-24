package mr.supnum.bib;

/** Emprunt enregistré : numéro attribué par le service et date de retour attendue. */
public class Emprunt {

    public String numero;
    public String dateRetour;

    /** Requis par JAXB, qui instancie la classe par réflexion avant de remplir ses champs. */
    public Emprunt() {
    }

    public Emprunt(String numero, String dateRetour) {
        this.numero = numero;
        this.dateRetour = dateRetour;
    }
}
