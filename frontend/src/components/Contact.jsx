import "../index.css";
import React, { useRef } from 'react';
import emailjs from '@emailjs/browser';

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
          <label>Name</label>
          <input type="text" name="username" className="w-full mb-4 p-2 bg-gray-800 border-2 border-gray-500 rounded-lg focus:border-blue-500 focus:bg-gray-900 focus:outline-none" />
          <label>Email</label>
          <input type="email" name="user_email" className="w-full mb-4 p-2 bg-gray-800 border-2 border-gray-500 rounded-lg focus:border-blue-500 focus:bg-gray-900 focus:outline-none" />
          <label>Message</label>
          <textarea name="message" className="w-full mb-4 p-2 h-24 bg-gray-800 border-2 border-gray-500 rounded-lg focus:border-blue-500 focus:bg-gray-900 focus:outline-none" />
          <input type="submit" value="Send" className="w-full p-2 bg-gray-700 text-white custom-button" />
        </form>
      </div>
    </div>
  );
};

export default Contact;