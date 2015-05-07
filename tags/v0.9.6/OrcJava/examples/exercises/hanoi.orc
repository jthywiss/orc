{--
Write a function to solve the
<ulink url="http://en.wikipedia.org/wiki/Tower_of_Hanoi">Towers of Hanoi</ulink>
problem. There are three pegs, numbered 0..2;
disks are to be moved from peg 0 to peg 2. Your
function should take as its argument the number
of disks initially on peg 0, and it should return
a list of moves, where a move is a tuple (source
peg number, destination peg number), e.g. <code>(0,1)</code>.
Since the point of this exercise is to practice Orc,
not solve puzzles, feel free to use the algorithm
given in Wikipedia.
--}

{--
Algorithm as described in
http://en.wikipedia.org/wiki/Tower_of_Hanoi
--}
def hanoi(n) =
  {- arguments: h(eight), f(rom peg),
     r(emaining peg), t(o peg), m(ove)s -}
  def move(1, f, r, t, ms) = (f,t):ms
  def move(h, f, r, t, ms) =
    move(h-1, f, t, r, ms) >ms>
    move(1, f, r, t, ms)   >ms>
    move(h-1, r, f, t, ms)
  reverse(move(n, 0, 1, 2, []))

hanoi(3)

{-
OUTPUT:
[(0, 2), (0, 1), (2, 1), (0, 2), (1, 0), (1, 2), (0, 2)]
-}