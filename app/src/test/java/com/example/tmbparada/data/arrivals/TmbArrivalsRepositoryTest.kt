package com.example.tmbparada.data.arrivals

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsResult
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.testing.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TmbArrivalsRepositoryTest {
    private val server = MockWebServer()
    private val clock = FakeClock(nowMillis = 50_000L)
    private val credentials = TmbCredentials(appId = "id", appKey = "key")

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    private fun repository(credentials: TmbCredentials = this.credentials) = TmbArrivalsRepository(
        apiClient = TmbApiClient(OkHttpClient(), credentials, server.url("/")),
        credentials = credentials,
        clock = clock,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `uses itransit with credentials and converts times to timestamps`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"timestamp": 0, "parades": [{"linies_trajectes": [{"nom_linia": "V29",
                   "desti_trajecte": "Diagonal Mar",
                   "propers_busos": [{"temps_arribada": 480000}, {"temps_arribada": 1200000}]}]}]}""",
            ),
        )

        val result = repository().fetchArrivals("2903")

        val request = server.takeRequest()
        assertEquals("/v1/itransit/bus/parades/2903", request.requestUrl?.encodedPath)
        assertEquals("id", request.requestUrl?.queryParameter("app_id"))
        assertEquals("key", request.requestUrl?.queryParameter("app_key"))
        assertEquals(
            ArrivalsResult.Success(
                listOf(
                    Arrival("V29", "Diagonal Mar", 50_000L + 480_000L),
                    Arrival("V29", "Diagonal Mar", 50_000L + 1_200_000L),
                ),
            ),
            result,
        )
    }

    @Test
    fun `falls back to ibus when itransit fails or changes shape`() = runTest {
        val ibus = """{"data": {"ibus": [{"line": "V15", "t-in-s": 90}]}}"""
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody(ibus))
        server.enqueue(MockResponse().setBody("""{"unexpected": true}"""))
        server.enqueue(MockResponse().setBody(ibus))
        val expected = ArrivalsResult.Success(listOf(Arrival("V15", null, 50_000L + 90_000L)))

        assertEquals(expected, repository().fetchArrivals("2903"))
        assertEquals(expected, repository().fetchArrivals("2903"))
        assertEquals(
            listOf(
                "/v1/itransit/bus/parades/2903",
                "/v1/ibus/stops/2903",
                "/v1/itransit/bus/parades/2903",
                "/v1/ibus/stops/2903",
            ),
            List(4) { server.takeRequest().requestUrl?.encodedPath },
        )
    }

    @Test
    fun `reports missing credentials without calling the api`() = runTest {
        val result = repository(TmbCredentials("", "")).fetchArrivals("2903")

        assertEquals(ArrivalsResult.Failure(RefreshError.MISSING_CREDENTIALS), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `rejected credentials are not retried on ibus`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        assertEquals(ArrivalsResult.Failure(RefreshError.UNAUTHORIZED), repository().fetchArrivals("1"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `reports failures of both endpoints`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setBody("<html>Service unavailable</html>"))

        val repository = repository()

        assertEquals(ArrivalsResult.Failure(RefreshError.SERVER), repository.fetchArrivals("1"))
        assertEquals(ArrivalsResult.Failure(RefreshError.INVALID_RESPONSE), repository.fetchArrivals("1"))
    }

    @Test
    fun `maps connection failures to a network error`() = runTest {
        val repository = repository()
        server.shutdown()

        assertEquals(ArrivalsResult.Failure(RefreshError.NETWORK), repository.fetchArrivals("1"))
    }
}
