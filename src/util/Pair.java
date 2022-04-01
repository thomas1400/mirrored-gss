package util;

public class Pair<T, U> {

  private final T first;
  private final U second;

  public Pair(T first, U second) {
    this.first = first;
    this.second = second;
  }

  public T getFirst() { return first; }
  public U getSecond() { return second; }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Pair p)) {
      return false;
    }

    return p.getFirst().equals(first) && p.getSecond().equals(second);
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(first.hashCode() + second.hashCode());
  }

  @Override
  public String toString() {
    return "<".concat(first.toString()).concat(", ").concat(second.toString()).concat(">");
  }
}
