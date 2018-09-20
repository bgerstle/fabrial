package com.eighthlight.fabrial.test.gen;

import org.quicktheories.core.Gen;

import static org.quicktheories.generators.Generate.characters;
import static org.quicktheories.generators.Generate.oneOf;
import static org.quicktheories.generators.SourceDSL.lists;

public class ArbitraryStrings {
  public static Gen<Character> az() { return characters('a', 'z'); }

  public static Gen<Character> AZ() { return characters('A', 'Z'); }

  public static Gen<Character> numeric() { return characters('0', '9'); }

  public static Gen<String> alphanumeric(int length) {
    return lists().of(
        oneOf(az(), AZ(), numeric())
    ).ofSize(length).map(chars ->
      chars.stream()
           .map(c -> Character.toString(c))
           .reduce("", String::concat)
    );
  }
}
