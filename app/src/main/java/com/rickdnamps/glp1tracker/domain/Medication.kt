package com.rickdnamps.glp1tracker.domain

import androidx.annotation.StringRes
import com.rickdnamps.glp1tracker.R

/**
 * Active ingredient with approximate pharmacokinetic parameters used for the
 * "estimated level" chart. Values come from the published product monographs
 * (half-life and time to peak after a subcutaneous dose).
 */
enum class Molecule(
    @StringRes val nameRes: Int,
    val halfLifeHours: Double,
    val tmaxHours: Double,
) {
    SEMAGLUTIDE(R.string.molecule_semaglutide, halfLifeHours = 168.0, tmaxHours = 56.0),
    TIRZEPATIDE(R.string.molecule_tirzepatide, halfLifeHours = 120.0, tmaxHours = 24.0),
    LIRAGLUTIDE(R.string.molecule_liraglutide, halfLifeHours = 13.0, tmaxHours = 11.0),
    DULAGLUTIDE(R.string.molecule_dulaglutide, halfLifeHours = 112.0, tmaxHours = 48.0),
    OTHER(R.string.molecule_other, halfLifeHours = 168.0, tmaxHours = 48.0),
}

enum class Medication(
    @StringRes val nameRes: Int,
    val molecule: Molecule,
    val defaultIntervalDays: Int,
    val commonDosesMg: List<Double>,
    val isOral: Boolean = false,
) {
    OZEMPIC(R.string.med_ozempic, Molecule.SEMAGLUTIDE, 7, listOf(0.25, 0.5, 1.0, 2.0)),
    WEGOVY(R.string.med_wegovy, Molecule.SEMAGLUTIDE, 7, listOf(0.25, 0.5, 1.0, 1.7, 2.4, 7.2)),
    WEGOVY_PILL(R.string.med_wegovy_pill, Molecule.SEMAGLUTIDE, 1, listOf(1.5, 4.0, 9.0, 25.0), isOral = true),
    RYBELSUS(R.string.med_rybelsus, Molecule.SEMAGLUTIDE, 1, listOf(3.0, 7.0, 14.0), isOral = true),
    MOUNJARO(R.string.med_mounjaro, Molecule.TIRZEPATIDE, 7, listOf(2.5, 5.0, 7.5, 10.0, 12.5, 15.0)),
    ZEPBOUND(R.string.med_zepbound, Molecule.TIRZEPATIDE, 7, listOf(2.5, 5.0, 7.5, 10.0, 12.5, 15.0)),
    SAXENDA(R.string.med_saxenda, Molecule.LIRAGLUTIDE, 1, listOf(0.6, 1.2, 1.8, 2.4, 3.0)),
    VICTOZA(R.string.med_victoza, Molecule.LIRAGLUTIDE, 1, listOf(0.6, 1.2, 1.8)),
    TRULICITY(R.string.med_trulicity, Molecule.DULAGLUTIDE, 7, listOf(0.75, 1.5, 3.0, 4.5)),
    SEMAGLUTIDE_COMPOUNDED(R.string.med_semaglutide_compounded, Molecule.SEMAGLUTIDE, 7, listOf(0.25, 0.5, 1.0, 1.7, 2.4)),
    TIRZEPATIDE_COMPOUNDED(R.string.med_tirzepatide_compounded, Molecule.TIRZEPATIDE, 7, listOf(2.5, 5.0, 7.5, 10.0, 12.5, 15.0)),
    OTHER(R.string.med_other, Molecule.OTHER, 7, emptyList());

    companion object {
        fun fromName(name: String?): Medication =
            entries.firstOrNull { it.name == name } ?: OZEMPIC
    }
}
