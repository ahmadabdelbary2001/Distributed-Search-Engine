using Microsoft.AspNetCore.Mvc;

namespace MvcWebServer.Controllers
{
    [Route("api/web")]
    [ApiController]
    public class WebServerController : ControllerBase
    {
        // تخزين عنوان الـ Coordinator (يمكنك تخزينه في مكان آخر حسب الحاجة)
        private static string coordinatorAddress;

        /// <summary>
        /// Endpoint لتحديث عنوان الـ Coordinator.
        /// </summary>
        [HttpPost("coordinator")]
        public IActionResult UpdateCoordinatorAddress([FromBody] string newCoordinatorAddress)
        {
            coordinatorAddress = newCoordinatorAddress;
            Console.WriteLine("Updated coordinator address to: " + newCoordinatorAddress);
            return Ok();
        }

        /// <summary>
        /// Endpoint لاسترجاع عنوان الـ Coordinator الحالي.
        /// </summary>
        [HttpGet("coordinator")]
        public ActionResult<string> GetCoordinatorAddress()
        {
            if (string.IsNullOrEmpty(coordinatorAddress))
            {
                return "Coordinator not available.";
            }
            return coordinatorAddress;
        }
    }
}
