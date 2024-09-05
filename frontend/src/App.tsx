import React from 'react';
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Navbar from './components/Navbar';
import BookList from './components/BookList';
import About from './components/About';
import Contact from './components/Contact';
import './App.css';

function App() {
  return (
<div className="App bg-backgroundColor-primary min-h-screen">
<BrowserRouter>
        <Navbar />
        <Routes>
          <Route path="/" element={<BookList />} />
          <Route path="/about" element={<About />} />
          <Route path="/contact" element={<Contact />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
