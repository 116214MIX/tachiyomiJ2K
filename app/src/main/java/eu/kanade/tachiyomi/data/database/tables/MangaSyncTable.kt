package eu.kanade.tachiyomi.data.database.tables

object MangaSyncTable {

    const val TABLE = "manga_sync"

    const val COL_ID = "tr_id"

    const val COL_MANGA_ID = "tr_manga_id"

    const val COL_SYNC_ID = "tr_sync_id"

    const val COL_REMOTE_ID = "tr_remote_id"

    const val COL_TITLE = "tr_title"

    const val COL_LAST_CHAPTER_READ = "tr_last_chapter_read"

    const val COL_STATUS = "tr_status"

    const val COL_SCORE = "tr_score"

    const val COL_TOTAL_CHAPTERS = "tr_total_chapters"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_SYNC_ID INTEGER NOT NULL,
            $COL_REMOTE_ID INTEGER NOT NULL,
            $COL_TITLE TEXT NOT NULL,
            $COL_LAST_CHAPTER_READ INTEGER NOT NULL,
            $COL_TOTAL_CHAPTERS INTEGER NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_SCORE FLOAT NOT NULL,
            UNIQUE ($COL_MANGA_ID, $COL_SYNC_ID) ON CONFLICT REPLACE,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""

}
