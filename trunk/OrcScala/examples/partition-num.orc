{-
author: Amin Shali
date: Wednesday, June 09 2010
-}

{-
Partition an integer to sum of `d' numbers and print all the possible combinations
-}

def seq(Integer, Integer) :: Integer
def seq(m, n) = if (m <: n) then (m | seq(m+1, n)) else stop

{-
relpacing >= with > would cause the algorithm to eliminate repeated numbers in a list
ignoring the m would cause the algorithm to generate all permutation
-}

def partition(Integer, Integer) :: List[Integer]
def partition(n, d) =
	def p(Integer, Integer, List[Integer], Integer) :: List[Integer]
  def p(n, d, l, m) = upto(n) >x> if (x+1>=m) then aux(n-x, d, (x+1):l, x+1) else stop
	def aux(Integer, Integer, List[Integer], Integer) :: List[Integer]
  def aux(s, d, l, m) =
    if (d = 1) then (
      if (s >= m) then s:l
      else stop
    )
    else (
      if (d :> s) then stop
      else p(s-1, d-1, l, m)
    )
  aux(n, d, [], 0)


println("(10, 3):") >> partition(10, 3)


{-
OUTPUT:
(10, 3):
[8, 1, 1]
[7, 2, 1]
[6, 2, 2]
[6, 3, 1]
[5, 3, 2]
[5, 4, 1]
[4, 3, 3]
[4, 4, 2]
-}
