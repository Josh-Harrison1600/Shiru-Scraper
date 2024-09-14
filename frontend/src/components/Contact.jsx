import "../index.css";
import React, { useRef } from 'react';
import emailjs from '@emailjs/browser';
import 'animate.css';

export const Contact = () => {
  const form = useRef();

  const sendEmail = (e) => {
    e.preventDefault();

    emailjs
      .sendForm(
        process.env.REACT_APP_EMAILJS_SERVICE_ID,
        process.env.REACT_APP_EMAILJS_TEMPLATE_ID,
        form.current,
        process.env.REACT_APP_EMAILJS_PUBLIC_KEY
      )
      .then(
        () => {
          console.log('SUCCESS! Email Sent.');
        },
        (error) => {
          console.log('FAILED...', error.text);
        }
      );
  };

  return (
    <div className="py-10 flex justify-center items-start relative"> {/* Flex container */}
      <div className="flex flex-col max-w-lg w-full"> {/* Container for form, flex-col to stack form elements */}
        <form ref={form} onSubmit={sendEmail} className="w-full text-gray-200 font-roboto">{/* Full-width form */}
          <h1 className="text-white text-3xl mb-8 animate__animated animate__backInDown">Contact Us</h1>
          <div className="animate__animated animate__backInUp">
          <label className="block text-left mb-2">Name</label>
          <input type="text" name="username" className="w-full mb-4 p-2 bg-gray-800 border-2 border-gray-500 rounded-lg focus:border-blue-500 focus:bg-gray-900 focus:outline-none" 
            id="name"
            placeholder="Enter your name"
          />
          <label className="block text-left mb-2">Email</label>
          <input type="email" name="user_email" className="w-full mb-4 p-2 bg-gray-800 border-2 border-gray-500 rounded-lg focus:border-blue-500 focus:bg-gray-900 focus:outline-none" 
            id="email"
            placeholder="Enter your email"
          />
          <label className="block text-left mb-2">Message</label>
          <textarea name="message" className="w-full mb-4 p-2 h-24 bg-gray-800 border-2 border-gray-500 rounded-lg focus:border-blue-500 focus:bg-gray-900 focus:outline-none" 
            id="message"
            placeholder="Enter your message"
          />
          <input type="submit" value="Send" className="w-full p-3  text-white transition ease-in-out delay-100 bg-gray-700 hover:translate-y-1 hover:scale-110 hover:bg-blue-600 duration-300" />
          </div>
        </form>
      </div>
    </div>
    
  );
};

export default Contact;