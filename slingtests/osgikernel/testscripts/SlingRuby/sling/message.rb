#!/usr/bin/env ruby

require 'digest/sha1'

module SlingMessage

  class MessageManager

    def initialize(sling)
      @sling = sling
    end

    def create(name, type, box = "drafts", props = {})
      home = @sling.get_user().home_folder_for()
      return @sling.execute_post(@sling.url_for("#{home}/message.create.html"), props.update("sakai:type" => type, "sakai:to" => name, "sakai:sendstate" => "pending", "sakai:messagebox" => box))
    end
 
    def send(messageId)
      sha1 = Digest::SHA1.hexdigest(messageId)
      path = "" + sha1[0, 2] + "/" + sha1[2, 2] + "/" + sha1[4,2]+ "/" + sha1[6,2] + "/" + messageId
      return @sling.execute_post(@sling.url_for("#{home}/message/#{path}.html"), "sakai:messagebox" => "outbox" )
    end

    def list_all_noopts()
      return @sling.execute_get(@sling.url_for("_user/message/all.json"))
    end

    def list_all(sortOn = "jcr:created", sortOrder = "descending" )
      return @sling.execute_get(@sling.url_for("_user/message/all.json?sortOn="+sortOn+"&sortOrder="+sortOrder))
    end

    def list_inbox(sortOn = "jcr:created", sortOrder = "descending" )
      return @sling.execute_get(@sling.url_for("_user/message/box.json?box=inbox&sortOn="+sortOn+"&sortOrder="+sortOrder))
    end

    def list_outbox(sortOn = "jcr:created", sortOrder = "descending" )
      return @sling.execute_get(@sling.url_for("_user/message/box.json?box=outbox&sortOn="+sortOn+"&sortOrder="+sortOrder))
    end
	
    
  end

end
