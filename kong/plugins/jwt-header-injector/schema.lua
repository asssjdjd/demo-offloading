local typedefs = require "kong.db.schema.typedefs"

return {
  name = "jwt-header-injector",
  fields = {
    { consumer = require("kong.db.schema.typedefs").no_consumer },
    { protocols = require("kong.db.schema.typedefs").protocols_http },
    { config = {
        type = "record",
        fields = {
        },
      },
    },
  },
}
