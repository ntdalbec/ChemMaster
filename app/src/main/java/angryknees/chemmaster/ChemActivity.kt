package angryknees.chemmaster

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Error

class ChemActivity : AppCompatActivity() {
    private val nodeChildrenContainers = mutableMapOf<ChemDispenser.ReagentNode, ViewGroup>()
    val INDENT_SIZE = 32
    val COLOR_PRIMARY = Color.WHITE
    val COLOR_ALT = Color.GRAY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chem)

        val chemName = intent.getStringExtra("chemName")
        val dispenser = ChemDispenser.getInstance()
        val chemAmount = intent.getIntExtra("chemAmount", ChemDispenser.maxAmountUnder(dispenser[chemName]
                ?: throw Error("Invalid Chem name"), 100))

        dispenser.reagentWalkerFrom(chemName, chemAmount).iterator().forEach(this::attach)
    }

    private fun attach(node: ChemDispenser.ReagentNode) {
        val (chem, _, parent) = node
        val isCompound = chem.isCompound
        val view = if (isCompound) buildCompound(node) else buildBase(node)
        val destination = nodeChildrenContainers[parent] ?: rootLinearLayout

        destination.addView(view)
    }

    private fun buildBase(node: ChemDispenser.ReagentNode): View {
        val (chem, amount) = node
        val name = chem.name

        val nameText = TextView(this)
        nameText.text = "$name $amount"
        nameText.setPadding(INDENT_SIZE, 0, 0, 0)

        return nameText
    }

    private fun buildCompound(node: ChemDispenser.ReagentNode): ViewGroup {
        val (chem, amount, parent, leftOver) = node
        val name = chem.name
        val temperature = chem.temperature

        val compoundReagentView = layoutInflater.inflate(R.layout.view_compound_reagent, null, false) as ViewGroup

        compoundReagentView.run {
            findViewById<TextView>(R.id.nameText).text = name
            findViewById<TextView>(R.id.amountText).text = amount.toString()
            findViewById<TextView>(R.id.leftOverText).text = if (leftOver != null)
                    "${amount + leftOver} - $leftOver"
                else
                    ""
            findViewById<TextView>(R.id.temperatureText).text = temperature?.toString() ?: ""
        }

        val reagentContainer = compoundReagentView.findViewById<LinearLayout>(R.id.reagentContainer)
        val parentViewColorDrawable = nodeChildrenContainers[parent]?.background as? ColorDrawable
        val parentColor = parentViewColorDrawable?.color
        val childrenColorDrawable = if (parentColor == COLOR_PRIMARY) { // state //
            ColorDrawable(COLOR_ALT)
        } else {
            ColorDrawable(COLOR_PRIMARY)
        }
        reagentContainer.background = childrenColorDrawable
        nodeChildrenContainers[node] = reagentContainer

        return compoundReagentView
    }
}
