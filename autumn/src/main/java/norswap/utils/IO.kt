package norswap.utils
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

// -------------------------------------------------------------------------------------------------

/**
 * Reads a complete file and returns its contents as a string.
 * @throws IOException see [Files.readAllBytes]
 * @throws InvalidPathException see [Paths.get]
 */
fun read_file (file: String)
    = String(Files.readAllBytes(Paths.get(file)))

// -------------------------------------------------------------------------------------------------

/**
 * Returns a list of all the paths that match the given glob pattern within the given directory.
 *
 * The pattern syntax is described in the doc of [FileSystem.getPathMatcher] --
 * the "glob:" part should be omitted.
 */
@Throws(IOException::class)
fun glob (pattern: String, directory: Path): List<Path>
{
    val matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern)

    val result = ArrayList<Path>()

    Files.walkFileTree (directory, object : SimpleFileVisitor<Path>()
    {
        override fun visitFile (file: Path, attrs: BasicFileAttributes): FileVisitResult
        {
            if (matcher.matches(file)) result.add(file)
            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed (file: Path, exc: IOException?)
            = FileVisitResult.CONTINUE
    })

    return result
}

// -------------------------------------------------------------------------------------------------