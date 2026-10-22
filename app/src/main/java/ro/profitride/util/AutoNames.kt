package ro.profitride.util

import ro.profitride.R
import ro.profitride.core.DocType
import ro.profitride.core.ExpenseType
import ro.profitride.core.MaintKind

/** Numele afișate pentru documente, lucrări de întreținere și cheltuieli. */
object AutoNames {
    fun doc(t: DocType): Int = when (t) {
        DocType.RCA -> R.string.doc_RCA
        DocType.ITP -> R.string.doc_ITP
        DocType.ROVINIETA -> R.string.doc_ROVINIETA
        DocType.CASCO -> R.string.doc_CASCO
        DocType.COPIE_CONFORMA -> R.string.doc_COPIE_CONFORMA
        DocType.PERMIS -> R.string.doc_PERMIS
        DocType.ATESTAT -> R.string.doc_ATESTAT
    }

    fun docSub(t: DocType): Int = when (t) {
        DocType.RCA -> R.string.doc_sub_RCA
        DocType.ITP -> R.string.doc_sub_ITP
        DocType.ROVINIETA -> R.string.doc_sub_ROVINIETA
        DocType.CASCO -> R.string.doc_sub_CASCO
        DocType.COPIE_CONFORMA -> R.string.doc_sub_COPIE_CONFORMA
        DocType.PERMIS -> R.string.doc_sub_PERMIS
        DocType.ATESTAT -> R.string.doc_sub_ATESTAT
    }

    fun docTip(t: DocType): Int = when (t) {
        DocType.RCA -> R.string.doc_tip_RCA
        DocType.ITP -> R.string.doc_tip_ITP
        DocType.ROVINIETA -> R.string.doc_tip_ROVINIETA
        DocType.CASCO -> R.string.doc_tip_CASCO
        DocType.COPIE_CONFORMA -> R.string.doc_tip_COPIE_CONFORMA
        DocType.PERMIS -> R.string.doc_tip_PERMIS
        DocType.ATESTAT -> R.string.doc_tip_ATESTAT
    }

    fun maint(k: MaintKind): Int = when (k) {
        MaintKind.ULEI -> R.string.maint_ULEI
        MaintKind.FILTRE -> R.string.maint_FILTRE
        MaintKind.DISTRIBUTIE -> R.string.maint_DISTRIBUTIE
        MaintKind.PLACUTE -> R.string.maint_PLACUTE
        MaintKind.ANVELOPE -> R.string.maint_ANVELOPE
        MaintKind.BATERIE -> R.string.maint_BATERIE
        MaintKind.REVIZIE -> R.string.maint_REVIZIE
    }

    fun expense(t: ExpenseType): Int = when (t) {
        ExpenseType.ALIMENTARE -> R.string.exp_ALIMENTARE
        ExpenseType.SERVICE -> R.string.exp_SERVICE
        ExpenseType.SPALATORIE -> R.string.exp_SPALATORIE
        ExpenseType.PARCARE -> R.string.exp_PARCARE
        ExpenseType.IMPOZIT -> R.string.exp_IMPOZIT
        ExpenseType.ALTUL -> R.string.exp_ALTUL
    }
}
