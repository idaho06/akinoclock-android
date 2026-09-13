package org.akinosoft.akinoclock.weather.parse

import org.akinosoft.akinoclock.weather.model.WeatherLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingJsonTest {

    private fun fixture(name: String) =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("weather/$name")) { "missing fixture $name" }
            .bufferedReader().readText()

    @Test
    fun `parses the madrid geocoding fixture, deduping admin1 against the city name`() {
        val results = GeocodingJson.parse(fixture("geocoding-madrid.json"))

        assertEquals(2, results.size)
        val first = results[0]
        // The fixture's admin1 ("Madrid") equals the city name, so it's deduped rather than
        // shown twice ("Madrid, Madrid, Spain" would read oddly to a user).
        assertEquals(WeatherLocation("Madrid, Spain", 40.4165, -3.70256, country = "Spain"), first)
    }

    @Test
    fun `admin1 equal to name is not repeated`() {
        val json = """{"results":[{"name":"Madrid","latitude":1.0,"longitude":2.0,"country":"Spain","admin1":"Madrid"}]}"""
        val results = GeocodingJson.parse(json)
        assertEquals("Madrid, Spain", results.single().name)
    }

    @Test
    fun `missing admin1 is tolerated`() {
        val json = """{"results":[{"name":"Nowhere","latitude":1.0,"longitude":2.0,"country":"Nowhereland"}]}"""
        val results = GeocodingJson.parse(json)
        assertEquals("Nowhere, Nowhereland", results.single().name)
    }

    @Test
    fun `no results yields an empty list`() {
        val results = GeocodingJson.parse(fixture("geocoding-no-results.json"))
        assertTrue(results.isEmpty())
    }

    @Test
    fun `malformed json throws`() {
        assertThrows(WeatherParseException::class.java) { GeocodingJson.parse("not json") }
    }
}
