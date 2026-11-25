package com.minimal.tvlauncher.data

/** Frases para el saludo en modo "cita estoica". */
object StoicQuotes {
    val all = listOf(
        "Hoy podés elegir cómo responder.",
        "Ya ocurrió. ¿Qué harás ahora?",
        "Cuando llegue la rabia, dejá que la primera frase se quede en tu cabeza.",
        "Un día difícil también admite un gesto amable.",
        "Tu ejemplo llega más lejos que tu consejo.",
        "Antes de ofenderte, preguntá qué quiso decir.",
        "Lo suficiente también merece un brindis.",
        "Dejá margen en tus planes para la vida.",
        "Si te equivocaste, todavía podés hablar con honestidad.",
        "El aplauso dura menos que el orgullo de haber sido justo.",
        "Hoy alguien va a pensar distinto. Escuchalo.",
        "Algunas cosas se ven mejor después de una noche de sueño.",
        "Que la prisa de otro no elija tu tono.",
        "Hasta una espera te deja elegir en qué pensar.",
        "Aprendé a disfrutar sin apretar el puño.",
        "Una disculpa a tiempo le ahorra trabajo al orgullo.",
        "¿Cuánto de este problema está pasando de verdad?",
        "Hacé bien la parte que lleva tu nombre.",
        "Lo que hoy te parece poco, un día te hizo ilusión.",
        "Podés sentir miedo y cumplir tu palabra.",
        "Antes de ganar la discusión, acordate de a quién querés.",
        "Hay promesas que se cumplen cuando nadie mira.",
        "Tratate con la paciencia que le tendrías a un aprendiz.",
        "La fortuna cambia. Tus principios te acompañan.",
        "Cada sí compromete un pedazo de tu día.",
        "A veces la valentía cabe en dos palabras. Me equivoqué.",
        "Dejá que el mal humor termine con vos.",
        "También se practica la justicia al repartir el mérito.",
        "Hoy tenés algo que mañana podrías extrañar.",
        "Un contratiempo merece una parte de tu día. ¿Cuánta?",
        "La vida ajena se ve fácil desde lejos.",
        "Hasta en la derrota podés elegir cómo dar la mano.",
        "Cuidá lo prestado, incluido este día.",
        "Tu palabra vale también en los asuntos pequeños.",
        "¿Necesitás más o ya podés disfrutar de esto?",
        "Cambiar de opinión también exige coraje.",
        "Cuando te elogien, acordate de quién te ayudó.",
        "La paciencia se practica justo cuando se acaba.",
        "Dejá para mañana las preocupaciones que son de mañana.",
        "Tené a mano una razón para hacer lo correcto.",
        "Se puede empezar de nuevo a mitad del día.",
        "Una puerta cerrada también acorta la lista de decisiones.",
        "Antes de juzgar un tropiezo, acordate del tuyo.",
        "Si el resultado tarda, igual podés cuidar el trabajo de hoy.",
        "Tu atención es una forma de pasar la vida.",
        "Disfrutá esta conversación. Nunca se repite igual.",
        "El carácter se entrena hasta en la fila del mercado.",
        "Decí hoy ese gracias que venís aplazando.",
        "Al final del día, preguntate a quién le hiciste bien.",
        "Mientras llega lo que esperás, ya estás viviendo.",
    )

    /** Una frase al azar, evitando repetir la [previous]. */
    fun random(previous: String? = null, rng: kotlin.random.Random = kotlin.random.Random): String {
        val pool = all.filter { it != previous }
        return pool[rng.nextInt(pool.size)]
    }
}
