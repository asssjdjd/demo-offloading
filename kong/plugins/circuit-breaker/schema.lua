local typedefs = require "kong.db.schema.typedefs"

return {
  name = "circuit-breaker",
  fields = {
    { consumer = typedefs.no_consumer },
    { protocols = typedefs.protocols_http },
    {
      config = {
        type = "record",
        fields = {
          { failure_threshold = { type = "integer", required = true, default = 5 } },
          { success_threshold = { type = "integer", required = true, default = 2 } },
          { open_timeout_ms = { type = "integer", required = true, default = 30000 } },
          { fallback_status = { type = "integer", required = true, default = 503 } },
          {
            fallback_body = {
              type = "string",
              required = true,
              default = '{"message":"Service temporarily unavailable"}'
            }
          }
        }
      }
    }
  }
}
