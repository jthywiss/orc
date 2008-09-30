{-
Download Lewis Carroll's "Alice Through the Looking Glass"
from Project Gutenberg http://www.gutenberg.org,
unzips it, finds the "JABBERWOCKY" poem, and sends
the first few lines to Google to spell check.
Prints out a list of corrections, with the number
of the word, the misspelled word, and a list of
suggested spellings.
-}
include "net.inc"

class InputStreamReader = java.io.InputStreamReader
class BufferedReader = java.io.BufferedReader
class ZipInputStream = java.util.zip.ZipInputStream
class URL = java.net.URL

def openURL(url) =
  URL(url) >url>
  url.openConnection().getInputStream()

def unzip(stream) =
  ZipInputStream(stream) >zip>
  zip.getNextEntry() >>
  zip

def skipto(reader, phrase) =
  reader.readLine() >line>
  if line = Null() then ""
  else if line.contains(phrase) then line
  else skipto(reader, phrase)
  
def spellCheck(word:words, i) =
  GoogleSpellUnofficial(word) >(_:_) as suggs>
  (i, word, suggs)
  | spellCheck(words, i+1)  
  
val url = "http://www.gutenberg.org/files/12/12.zip"

BufferedReader(InputStreamReader(unzip(openURL(url)))) >reader>
skipto(reader, "JABBERWOCKY") >>
reader.readLine() >>
map(lambda (_) = reader.readLine(), range(1, 5)) >lines>
unlines(lines).trim().split("\\s+") >words>
spellCheck(words, 1)
