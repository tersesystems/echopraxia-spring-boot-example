import * as std from "std";
alias std.strings as str;

# Documentation is found in std, i.e. str.starts_with? is in
# https://twineworks.github.io/tweakflow/modules/std.html#strings-starts_with?
# Note that starts_with? returns nil rather than false if a passed in field is nil

library echopraxia {

  # level: the logging level
  # fields: the dictionary of fields
  function evaluate: (string level, dict fields) ->
    fields[:request_remote_addr] != nil && str.starts_with?(fields[:request_remote_addr], "127");

}