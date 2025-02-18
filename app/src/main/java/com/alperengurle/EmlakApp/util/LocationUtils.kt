package com.alperengurle.EmlakApp.util

import android.content.Context
import org.json.JSONArray
import java.io.IOException

data class City(
    val id: String,
    val name: String
)

data class District(
    val id: String,
    val name: String,
    val cityId: String
)

data class Neighborhood(
    val id: String,
    val name: String,
    val districtId: String
)

object LocationUtils {
    fun getCities(context: Context): List<City> {
        val cities = mutableListOf<City>()
        try {
            val jsonString = context.assets.open("sehirler.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                cities.add(City(
                    id = obj.getString("sehir_id"),
                    name = obj.getString("sehir_adi")
                ))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return cities.sortedBy { it.name }
    }

    fun getDistricts(context: Context, cityId: String): List<District> {
        val districts = mutableListOf<District>()
        try {
            val jsonString = context.assets.open("ilceler.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getString("sehir_id") == cityId) {
                    districts.add(District(
                        id = obj.getString("ilce_id"),
                        name = obj.getString("ilce_adi"),
                        cityId = cityId
                    ))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return districts.sortedBy { it.name }
    }

    fun getNeighborhoods(context: Context, districtId: String): List<Neighborhood> {
        val neighborhoods = mutableListOf<Neighborhood>()
        for (i in 1..4) {
            try {
                val jsonString = context.assets.open("mahalleler-$i.json").bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                for (j in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(j)
                    if (obj.getString("ilce_id") == districtId) {
                        neighborhoods.add(Neighborhood(
                            id = obj.getString("mahalle_id"),
                            name = obj.getString("mahalle_adi"),
                            districtId = districtId
                        ))
                    }
                }
                if (neighborhoods.isNotEmpty()) break
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return neighborhoods.sortedBy { it.name }
    }
}