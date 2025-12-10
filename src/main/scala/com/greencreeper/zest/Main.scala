package com.greencreeper.zest

import com.greencreeper.zest.template.Template
import java.io.File

object Main extends App {

  case class User(name: String, age: Int, address: Address)
  case class Address(street: String, city: String)

  // Create a template file for demonstration
  val templateContent = """
    |<h1>User Profile</h1>
    |<p>Name: {{user.name}}</p>
    |<p>Age: {{user.age}}</p>
    |<p>Address: {{user.address.street}}, {{user.address.city}}</p>
    |<p>Favorite Color: {{user.favoriteColor}}</p>
  """.stripMargin

  val tempFile = File.createTempFile("user_profile_template", ".html")
  var missingFieldTempFile: File = null // Declare outside try block
  try {
    os.write.over(os.Path(tempFile.getAbsolutePath), templateContent)

    val alice = User("Alice Wonderland", 30, Address("Rabbit Hole", "Fantasy Land"))
    val context = Map("user" -> alice)

    val template = Template.fromFile(tempFile.getAbsolutePath)
    val renderedHtml = template.render(context)

    println(renderedHtml)

    // Demonstrate missing field handling
    val missingFieldTemplateContent = "Hello, {{user.nonExistent}}!"
    missingFieldTempFile = File.createTempFile("missing_field_template", ".txt") // Assign here
    os.write.over(os.Path(missingFieldTempFile.getAbsolutePath), missingFieldTemplateContent)
    val missingFieldTemplate = Template.fromFile(missingFieldTempFile.getAbsolutePath)
    println("\n--- Demonstrating missing field handling ---")
    println(missingFieldTemplate.render(context))

  } finally {
    tempFile.delete()
    if (missingFieldTempFile != null) { // Check for null before deleting
      missingFieldTempFile.delete()
    }
  }
}
