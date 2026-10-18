package com.example.tmbparada.data.catalog

import com.example.tmbparada.domain.BusLines
import com.example.tmbparada.domain.StopCatalog
import com.example.tmbparada.domain.model.BusStop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStopCatalog(private val dao: TransitDao) : StopCatalog {
    override fun search(query: String): Flow<List<BusStop>> =
        dao.searchStops(
            matchQuery = query.toFtsPrefixQuery(),
            exactStopCode = query.trim().toIntOrNull() ?: NO_STOP_CODE,
            limit = MAX_RESULTS,
        ).map { rows -> rows.map(StopSearchRow::toBusStop) }

    private companion object {
        const val MAX_RESULTS = 30
        const val NO_STOP_CODE = -1
    }
}

private fun StopSearchRow.toBusStop() = BusStop(
    code = stopCode.toString(),
    name = stopName,
    lines = lineNames.orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .sortedWith(BusLines.ordering),
)
