package angryknees.chemmaster

import java.io.Serializable

data class Chem(
        val name: String,
        val isCompound: Boolean,
        var formula: HashMap<Chem, Int>?,
        val description: String?,
        val adjustedProduct: Pair<Int, Int>?,
        val temperature: Int?,
        val treatmentFor: String?,
        val metabolismRate: String?,
        val overdoseThreshold: String?,
        val catalyst: Catalyst?,
        val addictionThreshold: String?,
        val damageDealt: String?
) : Serializable