package angryknees.chemmaster

import org.json.JSONArray
import java.util.*
import kotlin.collections.HashMap

class ChemDispenser {
    private val chemMap = mutableMapOf<String, Chem>()

    val chemNames: Set<String>
        get() = chemMap.keys

    val compoundNames: Set<String>
        get() = chemMap.entries
                .filter { it.value.isCompound }
                .map { it.key }
                .toSet()

    operator fun get(name: String) : Chem? = chemMap[name]
    fun reagentWalkerFrom(name: String, amount: Int): ReagentWalker = ReagentWalker(chemMap[name] ?: throw IllegalArgumentException("$name is not a chem that exists"), amount)
    fun reagentWalkerFrom(chem: Chem, amount: Int): ReagentWalker = ReagentWalker(chem, amount)

    companion object {
        private var INSTANCE: ChemDispenser? = null

        fun getInstance(): ChemDispenser = INSTANCE ?: throw Error("load must be called first before calling getInstance")

        fun load(jsonString: String) {
            INSTANCE = ChemDispenser(jsonString)
        }

        fun maxAmountUnder(chem: Chem, limit: Int): Int {
            val stepSize = totalPartsFromChem(chem)
            if (stepSize == 0) throw IllegalArgumentException("${chem.name} is not a compound")
            var max = 0
            for (x in 0..limit step stepSize) {
                if (x % 5 == 0) max = x
            }
            return max
        }

        fun totalPartsFromChem(chem: Chem): Int =
            chem.formula?.entries?.fold(0) { acc, reagent -> acc + reagent.value } ?: 0
    }

    inner class ReagentWalker(root: Chem, amount: Int) : Iterable<ReagentNode> {
        val stack = Stack<ReagentNode>()

        override fun iterator(): Iterator<ReagentNode> = object : Iterator<ReagentNode> {
            override fun hasNext(): Boolean = !stack.empty()

            override fun next(): ReagentNode {
                val currentNode = stack.pop()
                val reagents = currentNode.currentChem.formula?.entries
                if (reagents != null) {
                    val amountNeeded = if (currentNode.currentChem.adjustedProduct != null) {
                        val ( from, to ) = currentNode.currentChem.adjustedProduct
                        ceilOnFives((currentNode.amount.toDouble() / to) * from)
                    } else {
                        currentNode.amount
                    }

                    val totalParts = totalPartsFromChem(currentNode.currentChem)
                    val amountPerPart = ceilOnFives(amountNeeded.toDouble() / totalParts)
                    val leftOver = (totalParts * amountPerPart) - amountNeeded
                    currentNode.leftOver = if (leftOver != 0 ) leftOver else null

                    for (reagent in reagents) {
                        val amount = reagent.value * amountPerPart
                        val newReagentNode = ReagentNode(reagent.key, amount, currentNode)
                        stack.push(newReagentNode)
                    }
                }
                return currentNode
            }
        }

        init {
            val rootNode = ReagentNode(root, amount)
            stack.push(rootNode)
        }

    }

    data class ReagentNode(
            val currentChem: Chem,
            val amount: Int,
            val parentNode: ReagentNode? = null,
            var leftOver: Int? = null)

    private fun ceilOnFives(num: Double) : Int = (Math.ceil(num / 5) * 5).toInt()

    private constructor(jsonString: String) {
        val chemsJSONArray = JSONArray(jsonString)
        val formulaLinkageMap = mutableMapOf<String, Map<String, Int>>()

        for (i in 0 until chemsJSONArray.length()) {
            val chem = chemsJSONArray.getJSONObject(i)

            var temperature: Int? = null
            var catalyst: Catalyst? = null
            var adjustedProduct: Pair<Int, Int>? = null

            val name = chem.getString("Name")
            val isCompound = chem.getBoolean("IsCompound")

            val description = chem.optString("Description", null)
            val treatmentFor = chem.optString("Treatment for", null)
            val metabolismRate = chem.optString("Metabolism Rate", null)
            val overdoseThreshold = chem.optString("Overdose Threshold", null)
            val addictionThreshold = chem.optString("Addiction Threshold", null)
            val damageDealt = chem.optString("Damage dealt", null)

            if (chem.has("Adjusted Product")) {
                val ratio = chem.getJSONArray("Adjusted Product")
                val from = ratio.getInt(0)
                val to = ratio.getInt(1)

                adjustedProduct = Pair(from, to)
            }

            if (chem.has("Temperature")) {
                temperature = chem.optInt("Temperature")
            }

            if (chem.has("Catalyst")) {
                val catalystJSON = chem.getJSONObject("Catalyst")
                val catName = catalystJSON.getString("Name")
                val catAmount = catalystJSON.getInt("Amount")
                val catIsRatio = catalystJSON.getBoolean("IsRatio")
                catalyst = Catalyst(catName, catAmount, catIsRatio)
            }

            if (chem.has("Formula")) {
                val formulaJSON = chem.getJSONObject("Formula")

                val formula = mutableMapOf<String, Int>()
                for (key in formulaJSON.keys()) {
                    formula[key] = formulaJSON.getInt(key)
                }
                formulaLinkageMap[name] = formula
            }

            val parsedChem = Chem(
                    name,
                    isCompound,
                    null,
                    description,
                    adjustedProduct,
                    temperature,
                    treatmentFor,
                    metabolismRate,
                    overdoseThreshold,
                    catalyst,
                    addictionThreshold,
                    damageDealt
            )

            chemMap[parsedChem.name] = parsedChem
        }

        for (node in formulaLinkageMap.entries) {
            val linkage = mutableMapOf<Chem, Int>()
            for (edge in node.value.entries) {
                val child = chemMap.getValue(edge.key)
                linkage[child] = edge.value
            }
            chemMap[node.key]!!.formula = linkage as HashMap<Chem, Int>
        }
    }
}