package com.example.deteksiretakbetonv1.Data

data class Recognition(
    var id: String ,
    var title: String ,
    var confidence: Float ,
    val percent: Float = confidence*100
)  {




    override fun toString(): String {
        return "Prediksi = $title \n" +
               "Akurasi  = $percent"
    }
}