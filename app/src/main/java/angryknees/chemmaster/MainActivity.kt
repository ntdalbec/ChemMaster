package angryknees.chemmaster


import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.SearchView
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val chemViewMap = mutableMapOf<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val chemJSON = resources.openRawResource(R.raw.chems).bufferedReader().readText()
        ChemDispenser.load(chemJSON)
        val dispenser = ChemDispenser.getInstance()

        dispenser.compoundNames.sorted().forEach {
            val textView = TextView(this)
            textView.text = it
            textView.textSize = 24F
            textView.gravity = Gravity.CENTER
            textView.setPadding(0, 16, 0, 16)
            textView.setOnClickListener(this::chemClickHandler)
            rootLinearLayout.addView(textView)

            chemViewMap[it] = textView
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s != "") {
                    chemViewMap.entries.forEach {
                        it.value.visibility = if (it.key.contains(s, true)) View.VISIBLE else View.GONE
                    }
                } else {
                    chemViewMap.entries.forEach { it.value.visibility = View.VISIBLE }
                }
            }
        })
    }

    private fun chemClickHandler(v: View) {
        val textView = v as TextView
        val name = textView.text.toString()
        val chem = ChemDispenser.getInstance()[name]
        configAmountSelection(chem!!)
    }

    private fun configAmountSelection(chem: Chem) {
        val formulaParts = ChemDispenser.totalPartsFromChem(chem)
        val interval = formulaParts * 5
        val default = ChemDispenser.maxAmountUnder(chem, 100)
        amountSelectionSeek.progress = default

        amountSelectionText.text = "Select amount: $default"

        amountSelectionSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val quo = progress.toDouble() / interval
                    val round = Math.round(quo).toInt()
                    val newProgress = round * interval
                    seekBar?.progress = newProgress
                    amountSelectionText.text = "Select amount: $newProgress"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        amountSelectionButton.setOnClickListener {
            amountSelection.visibility = View.GONE
            val intent = newIntent(this, chem.name, amountSelectionSeek.progress)
            startActivity(intent)
        }

        amountSelection.visibility = View.VISIBLE
    }

    companion object {
        fun newIntent(context: Context, chemName: String, amount: Int): Intent {
            val intent = Intent(context, ChemActivity::class.java)
            intent.putExtra("chemName", chemName)
            intent.putExtra("chemAmount", amount)
            return intent
        }
    }
}