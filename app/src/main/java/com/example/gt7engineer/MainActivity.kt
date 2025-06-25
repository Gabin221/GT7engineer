package com.example.gt7engineer

import android.app.AlertDialog
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val apiKey = "use/your/value"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val envoyerBtn = findViewById<Button>(R.id.envoyerBtn)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val ajouterConfigBtn = findViewById<Button>(R.id.ajouterConfigBtn)
        val container = findViewById<LinearLayout>(R.id.containerConfigsPuissance)

        ajouterConfigBtn.setOnClickListener {
            val bloc = layoutInflater.inflate(R.layout.bloc_puissance_config, null)
            val typeSpinner = bloc.findViewById<Spinner>(R.id.spinner_type_admission)
            val regimeSpinner = bloc.findViewById<Spinner>(R.id.spinner_regime)

            typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = parent?.getItemAtPosition(position).toString()
                    regimeSpinner.visibility = if (selected == "Atmosphérique") View.GONE else View.VISIBLE
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            container.addView(bloc)
        }

        envoyerBtn.setOnClickListener {
            val prompt = buildPromptFromInputs()
            if (prompt == null) {
                Toast.makeText(this, "Champs manquants ou invalides", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            envoyerBtn.isEnabled = false
            progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val model = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = apiKey
                    )
                    val response = model.generateContent(prompt)
                    val reply = response.text ?: "Aucune réponse reçue"

                    runOnUiThread {
                        envoyerBtn.isEnabled = true
                        progressBar.visibility = View.GONE
                        showScrollableDialog(reply)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        envoyerBtn.isEnabled = true
                        progressBar.visibility = View.GONE
                        showScrollableDialog("Erreur : ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private fun buildPromptFromInputs(): String? {
        fun getText(id: Int) = findViewById<EditText>(id).text.toString().trim()

        val modele = getText(R.id.modele)
        val poids = getText(R.id.poids)
        val puissance = getText(R.id.puissance)
        val couple = getText(R.id.couple)
        val nbRapports = getText(R.id.nbRapports)
        val ppCible = getText(R.id.ppCible)
        val circuit = getText(R.id.nomCircuit)

        if (modele.isEmpty() || poids.isEmpty() || puissance.isEmpty() || couple.isEmpty() || nbRapports.isEmpty()) {
            return null
        }

        val container = findViewById<LinearLayout>(R.id.containerConfigsPuissance)
        val puissanceDetails = StringBuilder()

        for (i in 0 until container.childCount) {
            val bloc = container.getChildAt(i)
            val spinnerType = bloc.findViewById<Spinner>(R.id.spinner_type_admission)
            val spinnerRegime = bloc.findViewById<Spinner>(R.id.spinner_regime)
            val puissanceVal = bloc.findViewById<EditText>(R.id.input_puissance).text.toString().trim()
            val coupleVal = bloc.findViewById<EditText>(R.id.input_couple).text.toString().trim()

            val type = spinnerType.selectedItem.toString()
            val regime = if (spinnerRegime.visibility == View.VISIBLE)
                spinnerRegime.selectedItem.toString()
            else "toute la plage de régime"

            if (type.isNotEmpty() && puissanceVal.isNotEmpty() && coupleVal.isNotEmpty()) {
                puissanceDetails.append("- $type ($regime) : $puissanceVal ch, $coupleVal Nm\n")
            }
        }

        if (puissanceDetails.isEmpty()) return null

        val prompt = """
            Tu es un expert en réglage de voiture pour Gran Turismo 7.
            Voici les informations concernant la voiture que je veux préparer :
            - Modèle : $modele
            - Poids : $poids kg
            - Puissance max constructeur : $puissance ch
            - Couple max constructeur : $couple Nm
            - Nombre de rapports : $nbRapports
            ${if (ppCible.isNotEmpty()) "- PP cible : $ppCible" else ""}
            ${if (circuit.isNotEmpty()) "- Circuit : $circuit" else ""}

            Voici les différentes configurations moteur que je peux utiliser :
            $puissanceDetails

            Merci d'utiliser ces données pour me proposer un réglage optimal pour apprendre à piloter sans aide, avec un volant et des sensations réalistes. J'aimerais améliorer mes freinages, maîtriser le survirage/sous-virage et adapter les réglages à mon style.
        """.trimIndent()

        return prompt
    }

    private fun showScrollableDialog(message: String) {
        val scrollView = ScrollView(this)
        val textView = TextView(this)
        textView.text = message
        textView.setPadding(32, 32, 32, 32)
        textView.movementMethod = ScrollingMovementMethod()

        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("Réponse de Gemini")
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .show()
    }
}
