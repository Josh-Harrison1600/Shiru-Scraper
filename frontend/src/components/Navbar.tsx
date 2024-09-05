import React from 'react';
import { FaGithub } from 'react-icons/fa'; 
import { Link } from 'react-router-dom';

const Navbar: React.FC = () => {
    return (
        <nav className='bg-black p-6 relative'>
            <div className='container mx-auto flex items-center justify-end'>
                {/* Centered Title */}
                <div className='absolute left-1/2 transform -translate-x-1/2 text-white text-3xl font-bold font-roboto '>
                    <Link to='/' className='text-white text-3xl font-bold font-roboto'>
                        Shiru    
                    </Link>
                </div>

                {/* Links and GitHub icon on the right */}
                <div className='flex items-center space-x-6'>
                    <a href='/about' className='text-white hover:text-gray-300 font-roboto'>
                        About
                    </a>
                    <a href='/contact' className='text-white hover:text-gray-300 font-roboto'>
                        Contact
                    </a>
                    <a
                        href="https://github.com/Josh-Harrison1600"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-white hover:text-gray-300"
                    >
                        <FaGithub size={24} />
                    </a>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;
