class Api::RubyWebServiceController < Api::RestController

  def custom_method
    render :text => "Custom method output"
  end

  private
  
  def rest_call
    render :text => "Rest method output"
  end
  
end